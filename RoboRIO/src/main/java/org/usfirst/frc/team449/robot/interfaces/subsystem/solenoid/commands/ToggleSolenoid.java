package org.usfirst.frc.team449.robot.interfaces.subsystem.solenoid.commands;

import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.command.Command;
import org.usfirst.frc.team449.robot.interfaces.subsystem.solenoid.SolenoidSubsystem;
import org.usfirst.frc.team449.robot.util.Logger;

/**
 * A command that toggles the position of a piston.
 */
public class ToggleSolenoid extends Command {

	/**
	 * The subsystem to execute this command on.
	 */
	private SolenoidSubsystem subsystem;

	/**
	 * Default constructor
	 *
	 * @param subsystem The solenoid subsystem to execute this command on.
	 */
	public ToggleSolenoid(SolenoidSubsystem subsystem) {
		this.subsystem = subsystem;
	}

	/**
	 * Log when this command is initialized
	 */
	@Override
	protected void initialize() {
		Logger.addEvent("ToggleSolenoid init.", this.getClass());
	}

	/**
	 * Toggle the state of the piston.
	 */
	@Override
	protected void execute() {
		if (subsystem.getSolenoidPosition().equals(DoubleSolenoid.Value.kForward)) {
			subsystem.setSolenoid(DoubleSolenoid.Value.kReverse);
		} else {
			subsystem.setSolenoid(DoubleSolenoid.Value.kForward);
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
		Logger.addEvent("ToggleSolenoid end.", this.getClass());
	}

	/**
	 * Log when this command is interrupted.
	 */
	@Override
	protected void interrupted() {
		Logger.addEvent("ToggleSolenoid Interrupted!", this.getClass());
	}
}