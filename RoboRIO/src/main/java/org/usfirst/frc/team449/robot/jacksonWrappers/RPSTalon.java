package org.usfirst.frc.team449.robot.jacksonWrappers;

import com.ctre.CANTalon;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import edu.wpi.first.wpilibj.Notifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.usfirst.frc.team449.robot.Robot;
import org.usfirst.frc.team449.robot.components.CANTalonMPComponent;
import org.usfirst.frc.team449.robot.generalInterfaces.shiftable.Shiftable;
import org.usfirst.frc.team449.robot.generalInterfaces.simpleMotor.SimpleMotor;
import org.usfirst.frc.team449.robot.logger.Logger;
import org.usfirst.frc.team449.robot.other.MotionProfileData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component wrapper on the CTRE {@link CANTalon}, with unit conversions to/from RPS built in. Every non-unit-conversion
 * in this class takes arguments in post-gearing RPS.
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
public class RPSTalon implements SimpleMotor, Shiftable {

	/**
	 * The CTRE CAN Talon SRX that this class is a wrapper on
	 */
	@NotNull
	private final CANTalon canTalon;

	/**
	 * The counts per rotation of the encoder being used, or null if there is no encoder.
	 */
	@Nullable
	private final Integer encoderCPR;

	/**
	 * The type of encoder the talon uses, or null if there is no encoder.
	 */
	@Nullable
	private final CANTalon.FeedbackDevice feedbackDevice;

	/**
	 * The coefficient the output changes by after being measured by the encoder, e.g. this would be 1/70 if there was a
	 * 70:1 gearing between the encoder and the final output.
	 */
	private final double postEncoderGearing;

	/**
	 * The number of inches travelled per rotation of the motor this is attached to, or null if there is no encoder.
	 * Only used for Motion Profile unit conversions. {@link Double} so it throws a nullPointer if you try to use it
	 * without a value in the map.
	 */
	@Nullable
	private final Double inchesPerRotation;

	/**
	 * A list of all the gears this robot has and their settings.
	 */
	@NotNull
	private final Map<Integer, PerGearSettings> perGearSettings;

	/**
	 * The minimum number of points that must be in the bottom-level MP buffer before starting a profile.
	 */
	private final int minNumPointsInBottomBuffer;

	/**
	 * The motion profile motionProfileStatus of the Talon.
	 */
	@NotNull
	private final CANTalon.MotionProfileStatus motionProfileStatus;

	/**
	 * A notifier that moves points from the API-level buffer to the talon-level one.
	 */
	private final Notifier bottomBufferLoader;

	/**
	 * The period for bottomBufferLoader, in seconds.
	 */
	private final double updaterProcessPeriodSecs;

	/**
	 * The current gear this Talon is in
	 */
	private int currentGear;

	/**
	 * The settings currently being used by this Talon.
	 */
	@NotNull
	private PerGearSettings currentGearSettings;

	/**
	 * The time at which the motion profile status was last checked. Only getting the status once per tic avoids CAN
	 * traffic.
	 */
	private long timeMPStatusLastRead;

	/**
	 * Default constructor.
	 *
	 * @param port                       CAN port of this Talon.
	 * @param inverted                   Whether this Talon is inverted.
	 * @param reverseOutput              Whether to reverse the output (identical effect to inverting outside of
	 *                                   position PID)
	 * @param enableBrakeMode            Whether to brake or coast when stopped.
	 * @param fwdLimitSwitchNormallyOpen Whether the forward limit switch is normally open or closed. If this is null,
	 *                                   the forward limit switch is disabled.
	 * @param revLimitSwitchNormallyOpen Whether the reverse limit switch is normally open or closed. If this is null,
	 *                                   the reverse limit switch is disabled.
	 * @param fwdSoftLimit               The forward software limit. If this is null, the forward software limit is
	 *                                   disabled. TODO figure out units
	 * @param revSoftLimit               The reverse software limit. If this is null, the reverse software limit is
	 *                                   disabled. TODO figure out units
	 * @param postEncoderGearing         The coefficient the output changes by after being measured by the encoder, e.g.
	 *                                   this would be 1/70 if there was a 70:1 gearing between the encoder and the
	 *                                   final output. Defaults to 1.
	 * @param inchesPerRotation          The number of inches travelled per rotation of the motor this is attached to.
	 * @param currentLimit               The max amps this device can draw. If this is null, no current limit is used.
	 * @param maxClosedLoopVoltage       The voltage to scale closed-loop output based on, e.g. closed-loop output of 1
	 *                                   will produce this voltage, output of 0.5 will produce half, etc. This feature
	 *                                   compensates for low battery voltage.
	 * @param feedbackDevice             The type of encoder used to measure the output velocity of this motor. Can be
	 *                                   null if there is no encoder attached to this Talon.
	 * @param encoderCPR                 The counts per rotation of the encoder on this Talon. Can be null if
	 *                                   feedbackDevice is, but otherwise must have a value.
	 * @param reverseSensor              Whether or not to reverse the reading from the encoder on this Talon. Can be
	 *                                   null if feedbackDevice is, but otherwise must have a value.
	 * @param perGearSettings            The settings for each gear this motor has. Can be null to use default values
	 *                                   and gear # of zero. Gear numbers can't be repeated.
	 * @param startingGear               The gear to start in. Can be null to use startingGearNum instead.
	 * @param startingGearNum            The number of the gear to start in. Ignored if startingGear isn't null.
	 *                                   Defaults to the lowest gear.
	 * @param minNumPointsInBottomBuffer The minimum number of points that must be in the bottom-level MP buffer before
	 *                                   starting a profile. Defaults to 128.
	 * @param updaterProcessPeriodSecs   The period for the Notifier that moves points between the MP buffers, in
	 *                                   seconds. Defaults to 0.005.
	 * @param slaves                     The other {@link CANTalon}s that are slaved to this one.
	 */
	@JsonCreator
	public RPSTalon(@JsonProperty(required = true) int port,
	                @JsonProperty(required = true) boolean inverted,
	                boolean reverseOutput,
	                @JsonProperty(required = true) boolean enableBrakeMode,
	                @Nullable Boolean fwdLimitSwitchNormallyOpen,
	                @Nullable Boolean revLimitSwitchNormallyOpen,
	                @Nullable Double fwdSoftLimit,
	                @Nullable Double revSoftLimit,
	                @Nullable Double postEncoderGearing,
	                @Nullable Double inchesPerRotation,
	                @Nullable Integer currentLimit,
	                double maxClosedLoopVoltage,
	                @Nullable CANTalon.FeedbackDevice feedbackDevice,
	                @Nullable Integer encoderCPR,
	                @Nullable Boolean reverseSensor,
	                @Nullable List<PerGearSettings> perGearSettings,
	                @Nullable Shiftable.gear startingGear,
	                @Nullable Integer startingGearNum,
	                @Nullable Integer minNumPointsInBottomBuffer,
	                @Nullable Double updaterProcessPeriodSecs,
	                @Nullable List<SlaveTalon> slaves) {
		//Instantiate the base CANTalon this is a wrapper on.
		canTalon = new CANTalon(port);
		//Set this to false because we only use reverseOutput for slaves.
		canTalon.reverseOutput(reverseOutput);
		//Set inversion
		canTalon.setInverted(inverted);
		//Set brake mode
		canTalon.enableBrakeMode(enableBrakeMode);

		//Set fields
		this.inchesPerRotation = inchesPerRotation;
		this.updaterProcessPeriodSecs = updaterProcessPeriodSecs != null ? updaterProcessPeriodSecs : 0.005;
		this.minNumPointsInBottomBuffer = minNumPointsInBottomBuffer != null ? minNumPointsInBottomBuffer : 128;

		//Initialize
		this.motionProfileStatus = new CANTalon.MotionProfileStatus();
		this.timeMPStatusLastRead = 0;
		this.perGearSettings = new HashMap<>();

		//If given no gear settings, use the default values.
		if (perGearSettings == null || perGearSettings.size() == 0) {
			this.perGearSettings.put(0, new PerGearSettings());
		}
		//Otherwise, map the settings to the gear they are.
		else {
			for (PerGearSettings settings : perGearSettings) {
				this.perGearSettings.put(settings.getGear(), settings);
			}
		}

		//If the starting gear isn't given, assume we start in low gear.
		if (startingGear == null) {
			if (startingGearNum == null) {
				currentGear = Integer.MAX_VALUE;
				for (Integer gear : this.perGearSettings.keySet()) {
					if (gear < currentGear) {
						currentGear = gear;
					}
				}
			} else {
				currentGear = startingGearNum;
			}
		} else {
			currentGear = startingGear.getNumVal();
		}
		currentGearSettings = this.perGearSettings.get(currentGear);

		//Set up gear-based settings.
		setGear(currentGear);

		//Only enable the limit switches if it was specified if they're normally open or closed.
		boolean fwdSwitchEnable = false, revSwitchEnable = false;
		if (fwdLimitSwitchNormallyOpen != null) {
			canTalon.ConfigFwdLimitSwitchNormallyOpen(fwdLimitSwitchNormallyOpen);
			fwdSwitchEnable = true;
		}
		if (revLimitSwitchNormallyOpen != null) {
			canTalon.ConfigRevLimitSwitchNormallyOpen(revLimitSwitchNormallyOpen);
			revSwitchEnable = true;
		}
		canTalon.enableLimitSwitch(fwdSwitchEnable, revSwitchEnable);

		//Only enable the software limits if they were given a value.
		if (fwdSoftLimit != null) {
			canTalon.enableForwardSoftLimit(true);
			canTalon.setForwardSoftLimit(fwdSoftLimit);
		} else {
			canTalon.enableForwardSoftLimit(false);
		}
		if (revSoftLimit != null) {
			canTalon.enableReverseSoftLimit(true);
			canTalon.setReverseSoftLimit(revSoftLimit);
		} else {
			canTalon.enableReverseSoftLimit(false);
		}

		//Set up the feedback device if it exists.
		if (feedbackDevice != null) {
			this.feedbackDevice = feedbackDevice;
			canTalon.setFeedbackDevice(feedbackDevice);
			this.encoderCPR = encoderCPR;
			canTalon.reverseSensor(reverseSensor);
		} else {
			this.feedbackDevice = null;
			this.encoderCPR = null;
		}

		//postEncoderGearing defaults to 1
		this.postEncoderGearing = postEncoderGearing != null ? postEncoderGearing : 1.;

		//Set the current limit if it was given
		if (currentLimit != null) {
			canTalon.setCurrentLimit(currentLimit);
			canTalon.EnableCurrentLimit(true);
		} else {
			//If we don't have a current limit, disable current limiting.
			canTalon.EnableCurrentLimit(false);
		}

		//Set the nominal closed loop battery voltage. Different thing from NominalOutputVoltage.
		canTalon.setNominalClosedLoopVoltage(maxClosedLoopVoltage);

		//Set up MP notifier
		bottomBufferLoader = new Notifier(canTalon::processMotionProfileBuffer);

		if (slaves != null) {
			//Set up slaves.
			for (SlaveTalon slave : slaves) {
				CANTalon tmp = new CANTalon(slave.getPort());
				//To invert slaves, use reverseOutput. See section 9.1.4 of the TALON SRX Software Reference Manual.
				tmp.reverseOutput(slave.isInverted());
				//Don't use the other inversion options
				tmp.reverseSensor(false);
				tmp.setInverted(false);

				tmp.enableLimitSwitch(false, false);
				tmp.enableForwardSoftLimit(false);
				tmp.enableReverseSoftLimit(false);
				tmp.configNominalOutputVoltage(0, 0);
				tmp.configPeakOutputVoltage(12, -12);
				tmp.configMaxOutputVoltage(12);

				//Brake mode and current limiting don't automatically follow master, so we set them up for each slave.
				tmp.enableBrakeMode(enableBrakeMode);
				if (currentLimit != null) {
					tmp.setCurrentLimit(currentLimit);
					tmp.EnableCurrentLimit(true);
				} else {
					//If we don't have a current limit, disable current limiting.
					tmp.EnableCurrentLimit(false);
				}

				//Set the slave up to follow this talon.
				tmp.changeControlMode(CANTalon.TalonControlMode.Follower);
				tmp.set(port);
				tmp.enable();
			}
		}
	}

	/**
	 * Give a PercentVbus setpoint (set to PercentVbus mode and set)
	 *
	 * @param percentVbus percent of total voltage from [-1, 1]
	 */
	public void setPercentVbus(double percentVbus) {
		//Warn the user if they're setting Vbus to a number that's outside the range of values.
		if (Math.abs(percentVbus) > 1.0) {
			Logger.addEvent("WARNING: YOU ARE CLIPPING MAX PERCENT VBUS AT " + percentVbus, this.getClass());
			percentVbus = Math.signum(percentVbus);
		}

		//Switch to voltage mode
		canTalon.changeControlMode(CANTalon.TalonControlMode.PercentVbus);

		//Set the setpoint to the input given.
		canTalon.set(percentVbus);
	}

	/**
	 * @return The gear this subsystem is currently in.
	 */
	@Override
	public int getGear() {
		return currentGear;
	}

	/**
	 * Shift to a specific gear.
	 *
	 * @param gear Which gear to shift to.
	 */
	@Override
	public void setGear(int gear) {
		currentGear = gear;
		currentGearSettings = perGearSettings.get(gear);
		canTalon.configPeakOutputVoltage(currentGearSettings.getFwdPeakOutputVoltage(), currentGearSettings.getRevPeakOutputVoltage());
		canTalon.configNominalOutputVoltage(currentGearSettings.getFwdNominalOutputVoltage(), currentGearSettings.getRevNominalOutputVoltage());
		if (currentGearSettings.getMaxSpeed() != null) {
			//Put driving constants in slot 0
			canTalon.setPID(currentGearSettings.getkP(), currentGearSettings.getkI(), currentGearSettings.getkD(),
					1023. / RPSToNative(currentGearSettings.getMaxSpeed()), 0, currentGearSettings.getClosedLoopRampRate(), 0);
			//Put MP constants in slot 1
			canTalon.setPID(currentGearSettings.getMotionProfileP(), currentGearSettings.getMotionProfileI(), currentGearSettings.getMotionProfileD(),
					1023. / RPSToNative(currentGearSettings.getMaxSpeed()), 0, currentGearSettings.getClosedLoopRampRate(), 1);
			canTalon.setProfile(0);
		}
	}

	/**
	 * @return the max speed of the gear the talon is currently in, in RPS, as given in the map, or null if no value
	 * given.
	 */
	@Nullable
	public Double getMaxSpeed() {
		return currentGearSettings.getMaxSpeed();
	}

	/**
	 * Converts the velocity read by the talon's getSpeed() method to the RPS of the output shaft. Note this DOES
	 * account for post-encoder gearing.
	 *
	 * @param encoderReading The velocity read from the encoder with no conversions.
	 * @return The velocity of the output shaft, in RPS, when the encoder has that reading, or null if no encoder CPR
	 * was given.
	 */
	@Nullable
	public Double encoderToRPS(double encoderReading) {
		if (feedbackDevice == CANTalon.FeedbackDevice.CtreMagEncoder_Absolute || feedbackDevice == CANTalon.FeedbackDevice.CtreMagEncoder_Relative) {
			//CTRE encoders use RPM
			return RPMToRPS(encoderReading) * postEncoderGearing;
		} else {
			//All other feedback devices use native units.
			Double RPS = nativeToRPS(encoderReading);
			if (RPS == null) {
				return null;
			}
			return RPS * postEncoderGearing;
		}
	}

	/**
	 * Converts from the velocity of the output shaft to what the talon's getSpeed() method would read at that velocity.
	 * Note this DOES account for post-encoder gearing.
	 *
	 * @param RPS The velocity of the output shaft, in RPS.
	 * @return What the raw encoder reading would be at that velocity, or null if no encoder CPR was given.
	 */
	@Nullable
	public Double RPSToEncoder(double RPS) {
		if (feedbackDevice == CANTalon.FeedbackDevice.CtreMagEncoder_Absolute || feedbackDevice == CANTalon.FeedbackDevice.CtreMagEncoder_Relative) {
			//CTRE encoders use RPM
			return RPSToRPM(RPS) / postEncoderGearing;
		} else {
			//All other feedback devices use native units.
			Double encoderReading = RPSToNative(RPS);
			if (encoderReading == null) {
				return null;
			}
			return encoderReading / postEncoderGearing;
		}
	}

	/**
	 * Convert from output RPS to the CANTalon native velocity units. Note this DOES NOT account for post-encoder
	 * gearing.
	 *
	 * @param RPS The RPS velocity you want to convert.
	 * @return That velocity in CANTalon native units, or null if no encoder CPR was given.
	 */
	@Contract(pure = true)
	@Nullable
	private Double RPSToNative(double RPS) {
		if (encoderCPR == null) {
			return null;
		}
		return (RPS / 10) * (encoderCPR * 4); //4 edges per count, and 10 100ms per second.
	}

	/**
	 * Convert from CANTalon native velocity units to output rotations per second. Note this DOES NOT account for
	 * post-encoder gearing.
	 *
	 * @param nat A velocity in CANTalon native units.
	 * @return That velocity in RPS, or null if no encoder CPR was given.
	 */
	@Contract(pure = true)
	@Nullable
	private Double nativeToRPS(double nat) {
		if (encoderCPR == null) {
			return null;
		}
		return (nat / (encoderCPR * 4)) * 10; //4 edges per count, and 10 100ms per second.
	}

	/**
	 * Convert from RPM to RPS. Note this DOES NOT account for post-encoder gearing.
	 *
	 * @param rpm A velocity in RPM.
	 * @return That velocity in RPS.
	 */
	@Contract(pure = true)
	private double RPMToRPS(double rpm) {
		return rpm / 60.;
	}

	/**
	 * Convert from RPS to RPM. Note this DOES NOT account for post-encoder gearing.
	 *
	 * @param rps A velocity in RPS.
	 * @return That velocity in RPM.
	 */
	@Contract(pure = true)
	private double RPSToRPM(double rps) {
		return rps * 60.;
	}

	/**
	 * Get the velocity of the CANTalon in RPS
	 * <p>
	 * Note: This method is called getSpeed since the {@link CANTalon} method is called getSpeed. However, the output is
	 * signed and is actually a velocity.
	 *
	 * @return The CANTalon's velocity in RPS, or null if no encoder CPR was given.
	 */
	@Nullable
	public Double getSpeed() {
		return encoderToRPS(canTalon.getSpeed());
	}

	/**
	 * Give a velocity closed loop setpoint in RPS
	 * <p>
	 * Note: This method is called setSpeed since the {@link CANTalon} method is called getSpeed. However, the input
	 * argument is signed and is actually a velocity.
	 *
	 * @param velocitySp velocity setpoint in revolutions per second
	 */
	public void setSpeed(double velocitySp) {
		//Switch control mode to speed closed-loop
		canTalon.changeControlMode(CANTalon.TalonControlMode.Speed);
		canTalon.set(RPSToEncoder(velocitySp));
	}

	/**
	 * Get the current closed-loop velocity error in RPS. WARNING: will give garbage if not in velocity mode.
	 *
	 * @return The closed-loop error in RPS, or null if no encoder CPR was given.
	 */
	@Nullable
	public Double getError() {
		return encoderToRPS(canTalon.getError());
	}

	/**
	 * Get the current velocity setpoint of the Talon in RPS. WARNING: will give garbage if not in velocity mode.
	 *
	 * @return The closed-loop velocity setpoint in RPS, or null if no encoder CPR was given.
	 */
	@Nullable
	public Double getSetpoint() {
		return encoderToRPS(canTalon.getSetpoint());
	}

	/**
	 * Get the voltage the Talon is currently drawing from the PDP.
	 *
	 * @return Voltage in volts.
	 */
	public double getOutputVoltage() {
		return canTalon.getOutputVoltage();
	}

	/**
	 * Get the current the Talon is currently drawing from the PDP.
	 *
	 * @return Current in amps.
	 */
	public double getOutputCurrent() {
		return canTalon.getOutputCurrent();
	}

	/**
	 * Convert from native units read by an encoder to feet moved. Note this DOES account for post-encoder gearing.
	 *
	 * @param nativeUnits A distance native units as measured by the encoder.
	 * @return That distance in feet, or null if no encoder CPR or inches per rotation was given.
	 */
	@Nullable
	public Double nativeToFeet(double nativeUnits) {
		if (encoderCPR == null || inchesPerRotation == null) {
			return null;
		}
		double rotations = nativeUnits / (encoderCPR * 4) * postEncoderGearing;
		return rotations * (inchesPerRotation / 12.);
	}

	/**
	 * Convert a distance from feet to encoder reading in native units. Note this DOES account for post-encoder
	 * gearing.
	 *
	 * @param feet A distance in feet.
	 * @return That distance in native units as measured by the encoder, or null if no encoder CPR or inches per
	 * rotation was given.
	 */
	@Nullable
	public Double feetToNative(double feet) {
		if (encoderCPR == null || inchesPerRotation == null) {
			return null;
		}
		double rotations = feet / (inchesPerRotation / 12.);
		return rotations * (encoderCPR * 4) / postEncoderGearing;
	}

	/**
	 * Convert a velocity from feet per second to encoder units. Note this DOES account for post-encoder gearing.
	 *
	 * @param fps A velocity in feet per second
	 * @return That velocity in either native units or RPS, depending on the type of encoder, or null if no encoder CPR
	 * or inches per rotation was given.
	 */
	@Nullable
	public Double feetPerSecToNative(double fps) {
		if (inchesPerRotation == null) {
			return null;
		}
		return RPSToEncoder(fps / (inchesPerRotation / 12.));
	}

	/**
	 * Convert a velocity from encoder units to feet per second. Note this DOES account for post-encoder gearing.
	 *
	 * @param nativeUnits A velocity in either native units or RPS, depending on the type of encoder.
	 * @return That velocity in feet per second, or null if no encoder CPR or inches per rotation was given.
	 */
	@Nullable
	public Double nativeToFeetPerSec(double nativeUnits) {
		Double RPS = encoderToRPS(nativeUnits);
		if (inchesPerRotation == null || RPS == null) {
			return null;
		}
		return RPS * inchesPerRotation / 12.;
	}

	/**
	 * Set the velocity for the motor to go at.
	 *
	 * @param velocity the desired velocity, on [-1, 1].
	 */
	@Override
	public void setVelocity(double velocity) {
		if (getMaxSpeed() != null) {
			setSpeed(velocity * getMaxSpeed());
		} else {
			setPercentVbus(velocity);
		}
	}

	/**
	 * Enables the motor, if applicable.
	 */
	@Override
	public void enable() {
		canTalon.enable();
	}

	/**
	 * Disables the motor, if applicable.
	 */
	@Override
	public void disable() {
		canTalon.disable();
	}

	/**
	 * Set the velocity scaled to a given gear's max velocity. Used mostly when autoshifting.
	 *
	 * @param velocity The velocity to go at, from [-1, 1], where 1 is the max speed of the given gear.
	 * @param gear     The number of the gear to use the max speed from to scale the velocity.
	 */
	public void setGearScaledVelocity(double velocity, int gear) {
		if (currentGearSettings.getMaxSpeed() == null) {
			setPercentVbus(velocity);
		} else {
			setSpeed(perGearSettings.get(gear).getMaxSpeed() * velocity);
		}
	}

	/**
	 * Set the velocity scaled to a given gear's max velocity. Used mostly when autoshifting.
	 *
	 * @param velocity The velocity to go at, from [-1, 1], where 1 is the max speed of the given gear.
	 * @param gear     The gear to use the max speed from to scale the velocity.
	 */
	public void setGearScaledVelocity(double velocity, Shiftable.gear gear) {
		setGearScaledVelocity(velocity, gear.getNumVal());
	}

	/**
	 * @return The encoder position in native units.
	 */
	public int getPositionNative() {
		return canTalon.getEncPosition();
	}

	/**
	 * @return the position of the talon in feet, or null of inches per rotation wasn't given.
	 */
	public Double getPositionFeet() {
		return nativeToFeet(canTalon.getEncPosition());
	}

	/**
	 * Resets the position of the Talon to 0.
	 */
	public void resetPosition() {
		canTalon.setEncPosition(0);
	}

	/**
	 * A private utility method for updating motionProfileStatus with the current motion profile status. Makes sure that
	 * the status is only gotten once per tic, to avoid CAN traffic overload.
	 */
	private void updateMotionProfileStatus() {
		if (timeMPStatusLastRead < Robot.currentTimeMillis()) {
			canTalon.getMotionProfileStatus(motionProfileStatus);
			timeMPStatusLastRead = Robot.currentTimeMillis();
		}
	}

	/**
	 * Whether this talon is ready to start running a profile.
	 *
	 * @return True if minNumPointsInBottomBuffer points have been loaded or the top buffer is empty, false otherwise.
	 */
	public boolean readyForMP() {
		updateMotionProfileStatus();
		return motionProfileStatus.topBufferCnt == 0 || motionProfileStatus.btmBufferCnt >= minNumPointsInBottomBuffer;
	}

	/**
	 * Whether this talon has finished running a profile.
	 *
	 * @return True if the active point in the talon is the last point, false otherwise.
	 */
	public boolean MPIsFinished() {
		updateMotionProfileStatus();
		return motionProfileStatus.activePoint.isLastPoint;
	}

	/**
	 * Clear the underrun flag for motion profiles.
	 */
	public void clearMPUnderrun() {
		canTalon.clearMotionProfileHasUnderrun();
	}

	/**
	 * Reset all MP-related stuff, including all points loaded in both the API and bottom-level buffers.
	 */
	public void clearMP() {
		canTalon.clearMotionProfileHasUnderrun();
		canTalon.clearMotionProfileTrajectories();
	}

	/**
	 * Starts running the loaded motion profile.
	 */
	public void startRunningMP() {
		this.enable();
		canTalon.changeControlMode(CANTalon.TalonControlMode.MotionProfile);
		canTalon.set(CANTalon.SetValueMotionProfile.Enable.value);
	}

	/**
	 * Holds the current position point in MP mode.
	 */
	public void holdPositionMP() {
		canTalon.changeControlMode(CANTalon.TalonControlMode.MotionProfile);
		canTalon.set(CANTalon.SetValueMotionProfile.Hold.value);
	}

	/**
	 * Disables the talon and loads the given profile into the talon.
	 *
	 * @param data The profile to load.
	 */
	public void loadProfile(MotionProfileData data) {
		bottomBufferLoader.stop();
		//Reset the Talon
		disable();
		clearMP();

		//Read velocityOnly out here so we only have to call data.isVelocityOnly() once.
		boolean velocityOnly = data.isVelocityOnly();

		for (int i = 0; i < data.getData().length; ++i) {
			CANTalon.TrajectoryPoint point = new CANTalon.TrajectoryPoint();
			//Set parameters that are true for all points
			point.profileSlotSelect = 1;        // gain selection, we always put MP gains in slot 1.
			point.velocityOnly = velocityOnly;  // true => no position servo just velocity feedforward

			// Set all the fields of the profile point
			point.position = feetToNative(data.getData()[i][0]);
			point.velocity = feetPerSecToNative(data.getData()[i][1]);
			point.timeDurMs = (int) (data.getData()[i][2] * 1000.);
			point.zeroPos = i == 0; // If it's the first point, set the encoder position to 0.
			point.isLastPoint = (i + 1) == data.getData().length; // If it's the last point, isLastPoint = true

			// Send the point to the Talon's buffer
			if (!canTalon.pushMotionProfileTrajectory(point)) {
				//If sending the point doesn't work, log an error and exit.
				Logger.addEvent("Buffer full!", CANTalonMPComponent.class);
				System.out.println("Buffer full!");
				break;
			}
		}
		bottomBufferLoader.startPeriodic(updaterProcessPeriodSecs);
	}

	/**
	 * Stops all MP-related threads to save on CPU power. Run at the beginning of teleop.
	 */
	public void stopMPProcesses() {
		bottomBufferLoader.stop();
	}

	/**
	 * An object representing a slave {@link CANTalon} for use in the map.
	 */
	@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
	private static class SlaveTalon {

		/**
		 * The port number of this Talon.
		 */
		private final int port;

		/**
		 * Whether this Talon is inverted compared to its master.
		 */
		private final boolean inverted;

		/**
		 * Default constructor.
		 *
		 * @param port     The port number of this Talon.
		 * @param inverted Whether this Talon is inverted compared to its master.
		 */
		@JsonCreator
		public SlaveTalon(@JsonProperty(required = true) int port,
		                  @JsonProperty(required = true) boolean inverted) {
			this.port = port;
			this.inverted = inverted;
		}

		/**
		 * @return The port number of this Talon.
		 */
		public int getPort() {
			return port;
		}

		/**
		 * @return true if this Talon is inverted compared to its master, false otherwise.
		 */
		public boolean isInverted() {
			return inverted;
		}
	}

	/**
	 * An object representing the CANTalon settings that are different for each gear.
	 */
	private static class PerGearSettings {

		/**
		 * The gear number this is the settings for.
		 */
		private final int gear;

		/**
		 * The forwards and reverse peak output voltages.
		 */
		private final double fwdPeakOutputVoltage, revPeakOutputVoltage;

		/**
		 * The forwards and reverse nominal output voltages.
		 */
		private final double fwdNominalOutputVoltage, revNominalOutputVoltage;

		/**
		 * The closed loop ramp rate, in volts/sec. 0 means no ramp rate.
		 */
		private final double closedLoopRampRate;

		/**
		 * The maximum speed of the motor in this gear, in RPS. Can be null if not using PID in this gear.
		 */
		@Nullable
		private final Double maxSpeed;

		/**
		 * The PID constants for the motor in this gear. Ignored if maxSpeed is null.
		 */
		private final double kP, kI, kD;

		/**
		 * The PID constants for motion profiles in this gear. Ignored if maxSpeed is null.
		 */
		private final double motionProfileP, motionProfileI, motionProfileD;

		/**
		 * Default constructor.
		 *
		 * @param gearNum                 The gear number this is the settings for. Ignored if gear isn't null.
		 * @param gear                    The gear this is the settings for. Can be null.
		 * @param fwdPeakOutputVoltage    The peak output voltage for closed-loop modes in the forwards direction, in
		 *                                volts. Defaults to 12.
		 * @param revPeakOutputVoltage    The peak output voltage for closed-loop modes in the reverse direction, in
		 *                                volts. Defaults to -fwdPeakOutputVoltage.
		 * @param fwdNominalOutputVoltage The minimum output voltage for closed-loop modes in the forwards direction.
		 *                                This does not rescale, it just sets any output below this voltage to this
		 *                                voltage. Defaults to 0.
		 * @param revNominalOutputVoltage The minimum output voltage for closed-loop modes in the reverse direction.
		 *                                This does not rescale, it just sets any output below this voltage to this
		 *                                voltage. Defaults to -fwdNominalOutputVoltage.
		 * @param closedLoopRampRate      The closed loop ramp rate, in volts/sec. Can be null, and if it is, no ramp
		 *                                rate is used..
		 * @param maxSpeed                The maximum speed of the motor in this gear, in RPS. Can be null if not using
		 *                                PID in this gear.
		 * @param kP                      The proportional PID constant for the motor in this gear. Ignored if maxSpeed
		 *                                is null. Defaults to 0.
		 * @param kI                      The integral PID constant for the motor in this gear. Ignored if maxSpeed is
		 *                                null. Defaults to 0.
		 * @param kD                      The derivative PID constant for the motor in this gear. Ignored if maxSpeed is
		 *                                null. Defaults to 0.
		 * @param motionProfileP          The proportional PID constant for motion profiles in this gear. Ignored if
		 *                                maxSpeed is null. Defaults to 0.
		 * @param motionProfileI          The integral PID constant for motion profiles in this gear. Ignored if
		 *                                maxSpeed is null. Defaults to 0.
		 * @param motionProfileD          The derivative PID constant for motion profiles in this gear. Ignored if
		 *                                maxSpeed is null. Defaults to 0.
		 */
		@JsonCreator
		public PerGearSettings(int gearNum,
		                       @Nullable Shiftable.gear gear,
		                       @Nullable Double fwdPeakOutputVoltage,
		                       @Nullable Double revPeakOutputVoltage,
		                       @Nullable Double fwdNominalOutputVoltage,
		                       @Nullable Double revNominalOutputVoltage,
		                       @Nullable Double closedLoopRampRate,
		                       @Nullable Double maxSpeed,
		                       double kP,
		                       double kI,
		                       double kD,
		                       double motionProfileP,
		                       double motionProfileI,
		                       double motionProfileD) {
			this.gear = gear != null ? gear.getNumVal() : gearNum;
			this.fwdPeakOutputVoltage = fwdPeakOutputVoltage != null ? fwdPeakOutputVoltage : 12;
			this.revPeakOutputVoltage = revPeakOutputVoltage != null ? revPeakOutputVoltage : -this.fwdPeakOutputVoltage;
			this.fwdNominalOutputVoltage = fwdNominalOutputVoltage != null ? fwdNominalOutputVoltage : 0;
			this.revNominalOutputVoltage = revNominalOutputVoltage != null ? revNominalOutputVoltage : -this.fwdNominalOutputVoltage;
			if (closedLoopRampRate != null) {
				//The talons have a minimum closed loop ramp rate of 1.173 volts/sec, anything lower becomes 0 which is
				//no ramp rate. That's obviously not what someone who inputs 1 volt/sec wants, so we bump things up.
				this.closedLoopRampRate = Math.max(closedLoopRampRate, 1.173);
			} else {
				this.closedLoopRampRate = 0;
			}
			this.maxSpeed = maxSpeed;
			this.kP = kP;
			this.kI = kI;
			this.kD = kD;
			this.motionProfileP = motionProfileP;
			this.motionProfileI = motionProfileI;
			this.motionProfileD = motionProfileD;
		}

		/**
		 * Empty constructor that uses all default options.
		 */
		public PerGearSettings() {
			this(0, null, null, null, null, null, null, null, 0, 0, 0, 0, 0, 0);
		}

		/**
		 * @return The gear number this is the settings for.
		 */
		public int getGear() {
			return gear;
		}

		/**
		 * @return The peak output voltage for closed-loop modes in the forwards direction, in volts.
		 */
		public double getFwdPeakOutputVoltage() {
			return fwdPeakOutputVoltage;
		}

		/**
		 * @return The peak output voltage for closed-loop modes in the reverse direction, in volts.
		 */
		public double getRevPeakOutputVoltage() {
			return revPeakOutputVoltage;
		}

		/**
		 * @return The minimum output voltage for closed-loop modes in the forwards direction. This does not rescale, it
		 * just sets any output below this voltage to this voltage.
		 */
		public double getFwdNominalOutputVoltage() {
			return fwdNominalOutputVoltage;
		}

		/**
		 * @return The minimum output voltage for closed-loop modes in the reverse direction. This does not rescale, it
		 * just sets any output below this voltage to this voltage.
		 */
		public double getRevNominalOutputVoltage() {
			return revNominalOutputVoltage;
		}

		/**
		 * @return The closed loop ramp rate, in volts/sec.
		 */
		public double getClosedLoopRampRate() {
			return closedLoopRampRate;
		}

		/**
		 * @return The maximum speed of the motor in this gear, in RPS.
		 */
		@Nullable
		public Double getMaxSpeed() {
			return maxSpeed;
		}

		/**
		 * @return The proportional PID constant for the motor in this gear.
		 */
		public double getkP() {
			return kP;
		}

		/**
		 * @return The integral PID constant for the motor in this gear.
		 */
		public double getkI() {
			return kI;
		}

		/**
		 * @return The derivative PID constant for the motor in this gear.
		 */
		public double getkD() {
			return kD;
		}

		/**
		 * @return The proportional PID constant for motion profiles in this gear.
		 */
		public double getMotionProfileP() {
			return motionProfileP;
		}

		/**
		 * @return The integral PID constant for motion profiles in this gear.
		 */
		public double getMotionProfileI() {
			return motionProfileI;
		}

		/**
		 * @return The derivative PID constant for motion profiles in this gear.
		 */
		public double getMotionProfileD() {
			return motionProfileD;
		}
	}
}
