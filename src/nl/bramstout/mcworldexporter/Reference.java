package nl.bramstout.mcworldexporter;

public class Reference<T> {

	public T value;
	
	public Reference() {
		value = null;
	}
	
	public Reference(T value) {
		this.value = value;
	}
	
}
