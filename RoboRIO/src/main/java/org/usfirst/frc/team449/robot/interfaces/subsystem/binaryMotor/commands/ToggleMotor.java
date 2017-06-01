package org.usfirst.frc.team449.robot.interfaces.subsystem.binaryMotor.commands;

import edu.wpi.first.wpilibj.command.Command;
import org.usfirst.frc.team449.robot.interfaces.subsystem.binaryMotor.BinaryMotorSubsystem;
import org.usfirst.frc.team449.robot.util.Logger;

/**
 * A command that toggles the state of the motor between off and on.
 */
public class ToggleMotor extends Command {

	/**
	 * The subsystem to execute this command on.
	 */
	private BinaryMotorSubsystem subsystem;

	/**
	 * Default constructor
	 *
	 * @param subsystem The subsystem to execute this command on.
	 */
	public ToggleMotor(BinaryMotorSubsystem subsystem) {
		this.subsystem = subsystem;
	}

	/**
	 * Log when this command is initialized
	 */
	@Override
	protected void initialize() {
		Logger.addEvent("ToggleMotor init.", this.getClass());
	}

	/**
	 * Toggle the motor state.
	 */
	@Override
	protected void execute() {
		if (subsystem.isMotorOn()) {
			subsystem.turnMotorOff();
		} else {
			subsystem.turnMotorOn();
		}
	}

	/**
	 * Finish immediately because this is a state-change command.
	 *
	 * @return true
	 */
	@Override
	protected boolean isFinished() {
		return true;
	}

	/**
	 * Log when this command ends
	 */
	@Override
	protected void end() {
		Logger.addEvent("ToggleMotor end.", this.getClass());
	}

	/**
	 * Log when this command is interrupted.
	 */
	@Override
	protected void interrupted() {
		Logger.addEvent("ToggleMotor Interrupted!", this.getClass());
	}
}