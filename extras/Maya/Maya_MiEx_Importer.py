# BSD 3-Clause License
# 
# Copyright (c) 2024, Bram Stout Productions
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 
# 1. Redistributions of source code must retain the above copyright notice, this
#    list of conditions and the following disclaimer.
# 
# 2. Redistributions in binary form must reproduce the above copyright notice,
#    this list of conditions and the following disclaimer in the documentation
#    and/or other materials provided with the distribution.
# 
# 3. Neither the name of the copyright holder nor the names of its
#    contributors may be used to endorse or promote products derived from
#    this software without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import maya.cmds as cmds
try:
    from PySide6 import QtCore, QtWidgets, QtGui
    from shiboken6 import wrapInstance
except:
    from PySide2 import QtCore, QtWidgets, QtGui
    from shiboken2 import wrapInstance
import os.path
import maya.OpenMayaUI as omui
from maya.api import OpenMaya
import maya.mel
import json

class ProgressInterrupt(KeyboardInterrupt):

    def __init__(self, *args):
        super().__init__(*args)

MIEX_PROGRESS_PROGRESSBAR_COUNTER = 0

class ProgressBar():
    """Registers a progress bar in the main window.
    If there is already a progress bar active, then it will
    use that existing progress bar.
    
    This is used with the \"with\" statement.
    It will therefore automatically close."""

    def __init__(self, name, isInterruptable = False):
        self.name = name
        self.isInterruptable = isInterruptable
        self.value = 0
        self.status = name
    
    def __enter__(self):
        global MIEX_PROGRESS_PROGRESSBAR_COUNTER

        gMainProgressBar = maya.mel.eval('$tmpgMainProgressBar = $gMainProgressBar')
        if(MIEX_PROGRESS_PROGRESSBAR_COUNTER > 0):
            cmds.progressBar(gMainProgressBar, edit=True, status=self.name, 
                                progress=0)
        else:
            cmds.progressBar(gMainProgressBar, edit=True, beginProgress=True,
                                isInterruptable=self.isInterruptable,
                                status=self.name, minValue=0, maxValue=100)
        
        MIEX_PROGRESS_PROGRESSBAR_COUNTER += 1
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        global MIEX_PROGRESS_PROGRESSBAR_COUNTER
        MIEX_PROGRESS_PROGRESSBAR_COUNTER -= 1

        if(MIEX_PROGRESS_PROGRESSBAR_COUNTER <= 0):
            gMainProgressBar = maya.mel.eval('$tmpgMainProgressBar = $gMainProgressBar')
            cmds.progressBar(gMainProgressBar, edit=True, endProgress=True)

    def checkInterrupt(self):
        """Checks if the progress is interrupted, and if so,
        raise the ProgressInterrupt exception.
        This exception is derived from KeyboardInterrupt,
        which is derived from BaseException, so it is
        not caught by try-catch statements that catch Exception."""
        if(self.isInterrupted()):
            raise ProgressInterrupt()

    def isInterrupted(self):
        """Checks if the progress bar has been interrupted
        and returns true if that is the case."""
        gMainProgressBar = maya.mel.eval('$tmpgMainProgressBar = $gMainProgressBar')
        return cmds.progressBar(gMainProgressBar, query=True, isCancelled=True)
    
    def set(self, progress, status):
        """Sets the progress and status of the progress bar.
        The progress goes from 0 to 1."""
        newValue = int(progress * 100.0)
        if(newValue != self.value or self.status != status):
            self.value = newValue
            self.status = status
            gMainProgressBar = maya.mel.eval('$tmpgMainProgressBar = $gMainProgressBar')
            cmds.progressBar(gMainProgressBar, edit=True, progress=self.value, 
                                status=status)

    def setFromIndex(self, index, size, status):
        """Sets the progress and status of the progress bar.
        The progress is calculated from the index and size."""
        self.set(float(index) / float(size), status)

    def setProgress(self, progress):
        """Sets the progress of the progress bar.
        The progress goes from 0 to 1."""
        newValue = int(progress * 100.0)
        if(newValue != self.value):
            self.value = newValue
            gMainProgressBar = maya.mel.eval('$tmpgMainProgressBar = $gMainProgressBar')
            cmds.progressBar(gMainProgressBar, edit=True, progress=self.value)

    def setProgressFromIndex(self, index, size):
        """Sets the progress of the progress bar.
        The progress is calculated from the index and size."""
        self.setProgress(float(index) / float(size))

    def setStatus(self, status):
        """Sets the status of the progress bar."""
        if(self.status != status):
            self.status = status
            gMainProgressBar = maya.mel.eval('$tmpgMainProgressBar = $gMainProgressBar')
            cmds.progressBar(gMainProgressBar, edit=True, status=status)



class FileInput(QtWidgets.QWidget):
    """Shows a QLineEdit together with a browse button"""

    def __init__(self, directoryMode = False, fileFilter = None, open = False):
        """If directoryMode is True, you can create or select directories.
        If directoryMode is False, you can select files.
        fileFilter is passed to the dialog as the fileFilter"""
        super().__init__()
        self.directoryMode = directoryMode
        self.open = open
        self.fileFilter = fileFilter
        self.layout = QtWidgets.QHBoxLayout(self)
        self.layout.setContentsMargins(0,0,0,0)
        self.layout.setSpacing(0)

        self.input = QtWidgets.QLineEdit()
        self.layout.addWidget(self.input)

        self.browserButton = QtWidgets.QToolButton()
        self.browserButton.setToolButtonStyle(QtCore.Qt.ToolButtonIconOnly)
        self.browserButton.setIcon(QtGui.QIcon(":/fileOpen.png"))
        self.browserButton.clicked.connect(self.browse)
        self.layout.addWidget(self.browserButton)
    
    def browse(self):
        """Launch the browse dialog"""
        startFolder = self.getPath()
        if not os.path.exists(startFolder):
            startFolder = OpenMaya.MFileObject.getResolvedFullName(".").replace("\\", "/")
        if not os.path.isdir(startFolder):
            startFolder = os.path.dirname(startFolder)
        fileMode = 0
        if self.open:
            fileMode = 1
        if(self.directoryMode):
            fileMode = 3
        result = cmds.fileDialog2(fileFilter=self.fileFilter, fileMode=fileMode, 
                        setProjectBtnEnabled=True, startingDirectory=startFolder)
        if not result:
            return
        self.input.setText(result[0])
        

    def getPath(self):
        """Get the path specified in the QLineEdit"""
        return OpenMaya.MFileObject.getResolvedFullName(self.input.text()).replace("\\", "/")
    
    def setPath(self, path):
        """Set the path shown in the QLineEdit"""
        self.input.setText(path)

class MiExImporter(QtWidgets.QWidget):

    def __init__(self):
        main_window_ptr = omui.MQtUtil.mainWindow()
        if main_window_ptr is None:
            return None
        main_window_ptr = wrapInstance(int(main_window_ptr), QtWidgets.QWidget)
        QtWidgets.QWidget.__init__(self, main_window_ptr)

        self.setWindowFlags(QtCore.Qt.WindowFlags(QtCore.Qt.Window))
        self.setWindowTitle("MiEx Importer")

        self.layout = QtWidgets.QVBoxLayout(self)

        self.fileInputLabel = QtWidgets.QLabel("World file")
        self.fileInputLabel.setAlignment(QtCore.Qt.AlignHCenter)
        self.layout.addWidget(self.fileInputLabel)
        self.fileInput = FileInput(False, "*.usd", True)
        self.layout.addWidget(self.fileInput)

        self.layout.addSpacing(24)

        self.namespaceInputLabel = QtWidgets.QLabel("Namespace")
        self.namespaceInputLabel.setAlignment(QtCore.Qt.AlignHCenter)
        self.layout.addWidget(self.namespaceInputLabel)
        self.namespaceInput = QtWidgets.QLineEdit("World")
        self.layout.addWidget(self.namespaceInput)

        self.layout.addSpacing(24)

        self.variantInputLabel = QtWidgets.QLabel("Variant")
        self.variantInputLabel.setAlignment(QtCore.Qt.AlignHCenter)
        self.layout.addWidget(self.variantInputLabel)
        self.variantInput = QtWidgets.QComboBox()
        self.variantInput.addItem("Proxy")
        self.variantInput.addItem("Render")
        self.variantInput.setEditable(False)
        self.variantInput.setCurrentIndex(0)
        self.layout.addWidget(self.variantInput)

        self.layout.addSpacing(32)

        self.button = QtWidgets.QPushButton("Import")
        self.button.clicked.connect(self.importObj)
        self.button.setMinimumHeight(48)
        self.button.setMinimumWidth(500)
        self.layout.addWidget(self.button)

        self.adjustSize()
        self.setSizePolicy(QtWidgets.QSizePolicy.Fixed,QtWidgets.QSizePolicy.Fixed)
    
    def run(self, *args):
        self.setEnabled(True)

        self.fileInput.setPath("")
        self.show()
    
    def importObj(self):
        self.setEnabled(False)

        filePath = self.fileInput.getPath()
        if not os.path.exists(filePath):
            self.setEnabled(True)
            print("File does not exist!")
            return
        
        namespace = self.namespaceInput.text().replace(" ", "")
        if namespace == "":
            namespace = "World"

        variant = self.variantInput.currentText()

        MIEX_IMPORT(filePath, namespace, variant)

        cmds.confirmDialog(title="Done!", message="The world has successfully been imported.", button="Ok")

        self.hide()

try:
    MIEX_IMPORTER_WINDOW.run()
except:
    MIEX_IMPORTER_WINDOW = MiExImporter()
    MIEX_IMPORTER_WINDOW.run()

def MIEX_IMPORT(path, namespace, variant):
    variantOption = "proxy"
    if variant == "Render":
        variantOption = "render"
    
    if not cmds.namespace(exists=namespace):
        cmds.namespace(add=namespace)
    cmds.namespace(set=namespace)

    cmds.mayaUSDImport(file=path, primVariant= [( "/world", "MiEx_LOD", variantOption )], shadingMode=[("useRegistry","UsdPreviewSurface")], readAnimData=False, primPath="/")

    cmds.namespace(set=":")
    
    meshes = cmds.ls(namespace + ":**", type="mesh", long=True)
    for mesh in meshes:
        cmds.setAttr(mesh + ".displayColors", 0)
        try:
            cmds.setAttr(mesh + ".currentColorSet", "Cd", type="string")
        except:
            pass
        try:
            cmds.setAttr(mesh + ".aiExportColors", 1)
        except:
            pass
        if variant == "Render" and mesh.endswith("_proxyShape"):
            # For the render variant we want to not have the proxy meshes.
            parent = cmds.listRelatives(mesh, parent=True, fullPath=True)
            cmds.delete(parent)

    
    def setupMaterial(shadingEngine, data):
        if not cmds.objExists(shadingEngine):
            return
        
        # Let's rename it so that it has "SG" at the end of it.
        shadingEngine = cmds.rename(shadingEngine, shadingEngine + "SG")

        deleteIncomingNodes(shadingEngine)

        connectionsToMake = []

        network = data["network"]
        for name, nodeData in network.items():
            try:
                importNode(name, nodeData, connectionsToMake)
            except Exception as e:
                print(e)
                print("Could not import node " + name)
        
        for conn in connectionsToMake:
            try:
                cmds.connectAttr(conn[0], conn[1], force=True)
            except Exception as e:
                print(e)
                print("Could not make connection: ", conn)
        
        for name, attr in data["terminals"].items():
            try:
                inputAttr = attr
                inputAttr = inputAttr.split("/")
                inputAttr = inputAttr[len(inputAttr)-1]
                cmds.connectAttr(namespace + ":" + inputAttr, shadingEngine + "." + name)
            except Exception as e:
                print(e)
                print("Could not make connection: ", attr, shadingEngine + "." + name)


    def deleteIncomingNodes(node):
        connections = cmds.listConnections(node, d = False, s = True)
        for node in connections:
            # We only want to delete dgNodes and not dagNodes, so check for dagNodes
            types = cmds.nodeType(node, inherited=True)
            if "dagNode" in types:
                continue

            # Delete it
            cmds.delete(node)
    
    def importNode(name, data, connectionsToMake):
        classifications = cmds.getClassification(data["type"])
        node = None
        for classification in classifications:
            tokens = classification.split(":")
            for token in tokens:
                if token.startswith("drawdb"):
                    continue
                if "shader" in token:
                    node = cmds.shadingNode(data["type"], name=namespace + ":" + name, asShader=True, skipSelect=True)
                elif "texture" in token:
                    node = cmds.shadingNode(data["type"], name=namespace + ":" + name, asTexture=True, isColorManaged=True, skipSelect=True)
                elif "utility" in token:
                    node = cmds.shadingNode(data["type"], name=namespace + ":" + name, asUtility=True, skipSelect=True)
                break
        if node is None:
            node = cmds.shadingNode(data["type"], name=namespace + ":" + name, asUtility=True, skipSelect=True)

        if "attributes" in data:
            for attrName, attrData in data["attributes"].items():
                try:
                    importAttr(node, attrName, attrData, connectionsToMake)
                except Exception as e:
                    print(e)
                    print("Could not import attribute " + node + "." + attrName)
    
    def importAttr(node, name, data, connectionsToMake):
        if "value" in data:
            if isinstance(data["value"], list):
                if len(data["value"]) == 1:
                    cmds.setAttr(node + "." + name, data["value"][0])
                elif len(data["value"]) == 2:
                    cmds.setAttr(node + "." + name, data["value"][0], data["value"][1], type=data["type"])
                elif len(data["value"]) == 3:
                    cmds.setAttr(node + "." + name, data["value"][0], data["value"][1], data["value"][2], type=data["type"])
                elif len(data["value"]) == 4:
                    cmds.setAttr(node + "." + name, data["value"][0], data["value"][1], data["value"][2], data["value"][3], type=data["type"])
            else:
                if isinstance(data["value"], str):
                    cmds.setAttr(node + "." + name, data["value"], type="string")
                else:
                    cmds.setAttr(node + "." + name, data["value"])
        elif "connection" in data:
            inputAttr = data["connection"]
            inputAttr = inputAttr.split("/")
            inputAttr = inputAttr[len(inputAttr)-1]
            connectionsToMake.append((namespace + ":" + inputAttr, node + "." + name))
        elif "keyframes" in data:
            keyframes = data["keyframes"]
            numFrames = len(keyframes) / 2
            i = 0
            if data["type"] == "float":
                while i < numFrames:
                    cmds.setKeyframe(node, attribute=name, time=keyframes[i*2], value=keyframes[i*2+1], outTangentType="step")

                    i += 1
            elif data["type"] == "float2":
                childAttrs = cmds.attributeQuery(name, node=node, listChildren=True)
                numCompounds = len(childAttrs)
                j = 0
                while j < numCompounds:
                    i = 0
                    while i < numFrames:
                        cmds.setKeyframe(node, attribute=childAttrs[j], time=keyframes[i*2], value=keyframes[i*2+1][j], outTangentType="step")
                        i += 1
                    j += 1
            # Other types aren't support right now
    
    if(os.path.exists(path.replace(".usd", "_materials.json"))):
        # We have a JSON material definition file, so let's go through it.
        contents = {}
        with open(path.replace(".usd", "_materials.json"), encoding='utf-8') as fp:
            contents = json.load(fp)
        
        with ProgressBar("Importing Materials") as prog:
            numMats = len(contents)
            counter = 0
            for key, value in contents.items():
                prog.setProgressFromIndex(counter, numMats)
                counter += 1
                try:
                    setupMaterial(namespace + ":" + key, value)
                except Exception as e:
                    print(e)
                    print("Could not set up material " + key)
