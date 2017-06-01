package org.usfirst.frc.team449.robot.interfaces.subsystem.MotionProfile.commands;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Subsystem;
import org.usfirst.frc.team449.robot.Robot;
import org.usfirst.frc.team449.robot.interfaces.subsystem.MotionProfile.MPSubsystem;
import org.usfirst.frc.team449.robot.util.Logger;

/**
 * Runs the command that is currently loaded in the given subsystem.
 */
public class RunLoadedProfile extends Command {

	/**
	 * The amount of time this command is allowed to run for, in milliseconds.
	 */
	private long timeout;

	/**
	 * The time this command started running at.
	 */
	private long startTime;

	/**
	 * The subsystem to execute this command on.
	 */
	private MPSubsystem subsystem;

	/**
	 * Whether or not we're currently running the profile.
	 */
	private boolean runningProfile;


	/**
	 * Default constructor.
	 *
	 * @param subsystem The subsystem to execute this command on.
	 * @param timeout   The max amount of time this subsystem is allowed to run for, in seconds.
	 * @param require   Whether or not to require the subsystem this command is running on.
	 */
	public RunLoadedProfile(MPSubsystem subsystem, double timeout, boolean require) {
		this.subsystem = subsystem;
		//Require if specified.
		if (require) {
			requires((Subsystem) subsystem);
		}

		//Convert to milliseconds.
		this.timeout = (long) (timeout * 1000);

		runningProfile = false;
	}

	/**
	 * Record the start time.
	 */
	@Override
	protected void initialize() {
		//Record the start time.
		startTime = Robot.currentTimeMillis();

		runningProfile = false;
	}

	/**
	 * If the subsystem is ready to start running the profile and it's not running yet, start running it.
	 */
	@Override
	protected void execute() {
		if (subsystem.readyToRunProfile() && !runningProfile) {
			subsystem.startRunningLoadedProfile();
			runningProfile = true;
		}
	}

	/**
	 * Finish when the profile finishes or the timeout is reached.
	 *
	 * @return true if the profile is finished or the timeout has been exceeded, false otherwise.
	 */
	@Override
	protected boolean isFinished() {
		return subsystem.profileFinished() || (Robot.currentTimeMillis() - startTime > timeout);
	}

	/**
	 * Hold position and log on exit.
	 */
	@Override
	protected void end() {
		subsystem.holdPosition();
		Logger.addEvent("RunLoadedProfile end.", this.getClass());
	}

	/**
	 * Disable and log if interrupted.
	 */
	@Override
	protected void interrupted() {
		subsystem.disable();
		Logger.addEvent("RunLoadedProfile interrupted!", this.getClass());
	}
}