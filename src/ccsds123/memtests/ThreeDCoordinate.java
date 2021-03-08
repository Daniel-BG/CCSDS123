package ccsds123.memtests;

public class ThreeDCoordinate {
	public enum AdvancementType {VERTICAL, DIAGONAL};
	private int width, height, depth, swath;
	private int z, t;
	private AdvancementType at;
	
	public ThreeDCoordinate(int x, int y, int z, int width, int height, int depth, AdvancementType at) {
		this.z = z;
		this.t = y*width+x;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.swath = this.width * this.depth;
		this.at = at;
	}
	
	public boolean nextCoord() {
		switch (this.at) {
		case VERTICAL:
			this.z++;
			if (this.z == this.height) {
				this.z = 0;
				this.t++;
				if (this.t == this.swath) {
					this.t = 0;
					return false;
				}
			}
			return true;
			
		case DIAGONAL:
			
			if (z == 0) {
				if (this.t < this.height - 1) { //we are on first diagonals up to min(swath,height)
					this.z = this.t + 1;
					this.t = 0;
				} else { //normal case
					this.z = this.height - 1;
					this.t = this.t - this.height + 2;
				}
			} else if (t >= swath - 1) {
				if (z - 1 == this.height - 2) { //last sample
					this.z = 0;
					this.t = 0;
					return false;
				} else if (height <= swath) { //last diagonals
					this.t = this.t + this.z - this.height + 2;
					this.z = this.height - 1;
				} else { //last diagonals in case swath is smaller than height (can eliminate this if if height always <= swath
					this.z += this.t + 1;
					this.t = 0;
					if (z >= this.height) {
						this.t = this.z - (this.height - 1);
						this.z = this.height - 1;
					}
				}
			} else {
				this.z--;
				this.t++;
			}
			return true;
		default:
			throw new IllegalStateException("BAD");	
		}
	}
	
	public int getX() {
		return this.t % width;
	}
	
	public int getY() {
		return this.t / width;
	}
	
	public int getZ() {
		return z;
	}
	
	public int getTMod() {
		return this.t % this.height;
	}
	
	@Override
	public String toString() {
		return "(" + this.getZ() + "," + this.getX() + ") (" + this.getY() + ")";
	}


}
