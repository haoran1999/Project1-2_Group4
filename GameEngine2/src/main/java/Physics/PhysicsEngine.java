package Physics;

interface PhysicsEngine {
	void process(double dtime);
	void setStepSize(double h);
	double getStepSize();
	boolean passedFlag();
}
