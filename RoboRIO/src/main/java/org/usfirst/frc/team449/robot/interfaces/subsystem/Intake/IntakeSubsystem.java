package org.usfirst.frc.team449.robot.interfaces.subsystem.Intake;

/**
 * A subsystem used for intaking and possibly ejecting game pieces.
 */
public interface IntakeSubsystem {
	/**
	 * Get the mode of the intake
	 *
	 * @return off, in slow, in fast, out slow, out fast.
	 */
	IntakeMode getMode();

	/**
	 * Set the speed of the intake to one of 5 IntakeModes.
	 *
	 * @param mode off, in slow, in fast, out slow, out fast.
	 */
	void setMode(IntakeMode mode);

	enum IntakeMode {
		OFF, IN_SLOW, IN_FAST, OUT_SLOW, OUT_FAST
	}
}