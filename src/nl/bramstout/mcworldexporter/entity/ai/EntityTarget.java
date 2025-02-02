package nl.bramstout.mcworldexporter.entity.ai;

import nl.bramstout.mcworldexporter.entity.Entity;

public abstract class EntityTarget {

	public boolean move = true;
	public boolean look = true;
	public float minLookYaw = -60f;
	public float maxLookYaw = 60f;
	public float minLookPitch = -45f;
	public float maxLookPitch = 45f;
	public float maxRotationDelta = 10f;
	
	public abstract float getPosX(float time);
	public abstract float getPosY(float time);
	public abstract float getPosZ(float time);
	
	public static class EntityTargetBlock extends EntityTarget{
		
		private int x;
		private int y;
		private int z;
		
		public EntityTargetBlock(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		public float getPosX(float time) {
			return ((float) x) + 0.5f;
		}
		
		@Override
		public float getPosY(float time) {
			return y;
		}
		
		@Override
		public float getPosZ(float time) {
			return ((float) z) + 0.5f;
		}
		
	}
	
	public static class EntityTargetPosition extends EntityTarget{
		
		private float x;
		private float y;
		private float z;
		
		public EntityTargetPosition(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		public float getPosX(float time) {
			return x;
		}
		
		@Override
		public float getPosY(float time) {
			return y;
		}
		
		@Override
		public float getPosZ(float time) {
			return z;
		}
		
	}
	
	public static class EntityTargetEntity extends EntityTarget{
		
		private Entity entity;
		
		public EntityTargetEntity(Entity entity) {
			this.entity = entity;
		}
		
		public Entity getEntity() {
			return entity;
		}
		
		@Override
		public float getPosX(float time) {
			if(entity.getAnimation() != null)
				return entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
			return entity.getX();
		}
		
		@Override
		public float getPosY(float time) {
			if(entity.getAnimation() != null)
				return entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
			return entity.getY();
		}
		
		@Override
		public float getPosZ(float time) {
			if(entity.getAnimation() != null)
				return entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
			return entity.getZ();
		}
		
	}
	
}
