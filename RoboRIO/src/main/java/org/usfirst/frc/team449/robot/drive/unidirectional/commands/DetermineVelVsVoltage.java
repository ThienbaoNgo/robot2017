package org.usfirst.frc.team449.robot.drive.unidirectional.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.jetbrains.annotations.NotNull;
import org.usfirst.frc.team449.robot.drive.unidirectional.DriveUnidirectional;
import org.usfirst.frc.team449.robot.jacksonWrappers.YamlCommandWrapper;
import org.usfirst.frc.team449.robot.jacksonWrappers.YamlSubsystem;
import org.usfirst.frc.team449.robot.other.Logger;

/**
 * A command to run the robot at a range of voltages and record the velocity.
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
public class DetermineVelVsVoltage <T extends YamlSubsystem & DriveUnidirectional> extends YamlCommandWrapper {

	/**
	 * The subsystem to execute this command on.
	 */
	@NotNull
	private final T subsystem;

	/**
	 * How far, in feet, to drive for each trial.
	 */
	private final double distanceToDrive;

	/**
	 * How many trials to do for each voltage.
	 */
	private final int numTrials;

	/**
	 * A list of all the voltages to be tested, from (0, 1].
	 */
	private final double[] voltagesToTest;

	/**
	 * The maximum measured speed, in feet/sec, for the current trial.
	 */
	private double maxSpeedForTrial;

	/**
	 * The index, in the list of voltages to test, of the voltage currently being tested.
	 */
	private int voltageIndex;

	/**
	 * How many trials are left for the current voltage.
	 */
	private double trialsRemaining;

	/**
	 * The current sign of the output. Alternates every trial so we just drive back and forth.
	 */
	private int sign;

	/**
	 * Default constructor.
	 *
	 * @param subsystem The subsystem to execute this command on.
	 * @param distanceToDrive How far, in feet, to drive for each trial.
	 * @param numTrials How many trials to do for each voltage.
	 * @param voltagesToTest A list of all the voltages to be tested, from (0, 1].
	 */
	@JsonCreator
	public DetermineVelVsVoltage(@NotNull @JsonProperty(required = true) T subsystem,
	                             @JsonProperty(required = true) double distanceToDrive,
	                             @JsonProperty(required = true) int numTrials,
	                             @JsonProperty(required = true) double[] voltagesToTest) {
		this.subsystem = subsystem;
		this.distanceToDrive = distanceToDrive;
		this.numTrials = numTrials;
		this.voltagesToTest = voltagesToTest;
	}

	/**
	 * Reset the encoder position and variables.
	 */
	@Override
	protected void initialize() {
		subsystem.resetPosition();
		sign = 1;
		voltageIndex = 0;
		maxSpeedForTrial = 0;
		trialsRemaining = numTrials;
	}

	/**
	 * Update the max speed for this trial and check if this trial is finished.
	 */
	@Override
	protected void execute() {
		//Multiply each by sign so that only the movement in the correct direction is counted and leftover momentum from
		// the previous trial isn't.
		maxSpeedForTrial = Math.max(maxSpeedForTrial, (sign*subsystem.getLeftVel() + sign*subsystem.getRightVel())/2.);

		//Check if we've driven past the given distance
		boolean drivenDistance;
		if (sign == -1){
			drivenDistance = (subsystem.getLeftPos()+subsystem.getRightPos())/2. <= 0;
		} else {
			drivenDistance = (subsystem.getLeftPos()+subsystem.getRightPos())/2. >= distanceToDrive;
		}

		//If we've driven past, log the max speed and reset the variables.
		if (drivenDistance){
			//Log
			Logger.addEvent(Double.toString(maxSpeedForTrial), this.getClass());

			//Reset
			maxSpeedForTrial = 0;

			//Switch direction
			sign *= -1;

			//Finished a trial
			trialsRemaining--;

			//Go onto the next voltage if we've done enough trials
			if (trialsRemaining < 0){
				trialsRemaining = numTrials;
				voltageIndex++;

				//Exit if we've done all trials for all voltages
				if (voltageIndex >= voltagesToTest.length){
					return;
				}
			}

			//Set the output to the correct voltage and sign
			subsystem.setOutput(sign*voltagesToTest[voltageIndex], sign*voltagesToTest[voltageIndex]);
		}
	}

	/**
	 * Finish when all trials have been run.
	 *
	 * @return true if all trials have be run, false otherwise.
	 */
	@Override
	protected boolean isFinished() {
		return voltageIndex >= voltagesToTest.length;
	}

	/**
	 * Do nothing, no logging because we want to be able to use R's subset method to find the max speeds.
	 */
	@Override
	protected void end() {
		//Nothing!
	}

	/**
	 * Log when interrupted.
	 */
	@Override
	protected void interrupted() {
		Logger.addEvent("DetermineVelVsVoltage Interrupted!", this.getClass());
	}
}