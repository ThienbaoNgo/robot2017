package org.usfirst.frc.team449.robot.drive.talonCluster;

import com.ctre.CANTalon;
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import maps.org.usfirst.frc.team449.robot.components.ToleranceBufferAnglePIDMap;
import maps.org.usfirst.frc.team449.robot.components.UnitlessCANTalonSRXMap;
import org.usfirst.frc.team449.robot.components.NavxSubsystem;
import org.usfirst.frc.team449.robot.components.UnitlessCANTalonSRX;
import org.usfirst.frc.team449.robot.drive.DriveSubsystem;
import org.usfirst.frc.team449.robot.drive.talonCluster.commands.DefaultArcadeDrive;
import org.usfirst.frc.team449.robot.drive.talonCluster.commands.ExecuteProfile;
import org.usfirst.frc.team449.robot.oi.OI2017ArcadeGamepad;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * A drive with a cluster of any number of CANTalonSRX controlled motors on each side.
 */
public class TalonClusterDrive extends DriveSubsystem implements NavxSubsystem {

	public UnitlessCANTalonSRX rightMaster;
	public UnitlessCANTalonSRX leftMaster;
	public AHRS navx;
	public ToleranceBufferAnglePIDMap.ToleranceBufferAnglePID turnPID;
	public ToleranceBufferAnglePIDMap.ToleranceBufferAnglePID straightPID;
	public OI2017ArcadeGamepad oi;
	public DoubleSolenoid shifter;
	// TODO take this out after testing
	public CANTalon.MotionProfileStatus leftTPointStatus;
	public CANTalon.MotionProfileStatus rightTPointStatus;
	private long startTime;
	private String logFN;
	public boolean overrideNavX;

	private double maxSpeed;
	private final double PID_SCALE = 0.9;
	private double upTimeThresh, downTimeThresh;
	private boolean okToUpshift, okToDownshift;
	private double upshiftFwdDeadband;

	private long timeAboveShift, timeBelowShift, timeLastShifted;
	private Double shiftDelay;

	public boolean overrideAutoShift = false;

	boolean lowGear = true;    //we want to start in low gear

	double wheelDia, upshift, downshift;

	public TalonClusterDrive(maps.org.usfirst.frc.team449.robot.drive.talonCluster.TalonClusterDriveMap
			                         .TalonClusterDrive map, OI2017ArcadeGamepad oi) {
		super(map.getDrive());
		logFN = "/home/lvuser/logs/driveLog-" + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + "" +
				".csv";
		try (PrintWriter writer = new PrintWriter(logFN)) {
			writer.println("time,left,right,left error,right error,left setpoint,right setpoint");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.map = map;
		this.oi = oi;
		//this.navx = new AHRS(SPI.Port.kMXP);
		this.turnPID = map.getTurnPID();
		this.straightPID = map.getStraightPID();
		this.upTimeThresh = map.getUpTimeThresh();
		this.downTimeThresh = map.getDownTimeThresh();
		if (map.hasShiftDelay()) {
			this.shiftDelay = map.getShiftDelay();
		}
		okToUpshift = false;
		okToDownshift = true;
		overrideAutoShift = false;
		timeLastShifted = 0;
		upshiftFwdDeadband = map.getUpshiftFwdDeadband();
		if (map.hasShifter()) {
			this.shifter = new DoubleSolenoid(map.getModuleNumber(), map.getShifter().getForward(), map.getShifter()
					.getReverse());
		}
		maxSpeed = -1;

		rightMaster = new UnitlessCANTalonSRX(map.getRightMaster());
		leftMaster = new UnitlessCANTalonSRX(map.getLeftMaster());

		long upshiftTIme, downshiftTime;

		for (UnitlessCANTalonSRXMap.UnitlessCANTalonSRX talon : map.getRightSlaveList()) {
			UnitlessCANTalonSRX talonObject = new UnitlessCANTalonSRX(talon);
			talonObject.canTalon.changeControlMode(CANTalon.TalonControlMode.Follower);
			talonObject.canTalon.set(map.getRightMaster().getPort());
		}
		for (UnitlessCANTalonSRXMap.UnitlessCANTalonSRX talon : map.getLeftSlaveList()) {
			UnitlessCANTalonSRX talonObject = new UnitlessCANTalonSRX(talon);
			talonObject.canTalon.changeControlMode(CANTalon.TalonControlMode.Follower);
			talonObject.canTalon.set(map.getLeftMaster().getPort());
		}

		upshift = map.getUpshift();
		downshift = map.getDownshift();
		wheelDia = map.getWheelDiameter();

		// TODO take this out
		leftTPointStatus = new CANTalon.MotionProfileStatus();
		rightTPointStatus = new CANTalon.MotionProfileStatus();
	}

	/**
	 * Sets the left and right wheel speeds as a voltage percentage, not nearly as precise as PID.
	 *
	 * @param left  The left throttle, a number between -1 and 1 inclusive.
	 * @param right The right throttle, a number between -1 and 1 inclusive.
	 */
	public void setVBusThrottle(double left, double right) {
		leftMaster.setPercentVbus(left);
		rightMaster.setPercentVbus(-right);
	}

	private void setPIDThrottle(double left, double right) {
		if (overrideAutoShift) {
			leftMaster.setSpeed(PID_SCALE * (left * leftMaster.getMaxSpeed()));
			rightMaster.setSpeed(PID_SCALE * (right * rightMaster.getMaxSpeed()));
		} else {
			leftMaster.setSpeed(PID_SCALE * (left * leftMaster.getMaxSpeedHG()));
			rightMaster.setSpeed(PID_SCALE * (right * rightMaster.getMaxSpeedHG()));
		}
	}

	/**
	 * Allows the type of motor control used to be varied in testing.
	 *
	 * @param left  Left throttle value
	 * @param right Right throttle value
	 */
	public void setDefaultThrottle(double left, double right) {
		setPIDThrottle(clipToOne(left), clipToOne(right));
		//setVBusThrottle(left, right);
	}

	public void logData() {
		try (FileWriter fw = new FileWriter(logFN, true)) {
			StringBuilder sb = new StringBuilder();
			sb.append((System.nanoTime() - startTime) / Math.pow(10, 9));
			sb.append(",");
			sb.append(leftMaster.getSpeed());
			sb.append(",");
			sb.append(rightMaster.getSpeed());
			sb.append(",");
			sb.append(leftMaster.canTalon.getSpeed());
			sb.append(",");
			sb.append(rightMaster.canTalon.getSpeed());
			sb.append(leftMaster.getError());
			sb.append(",");
			sb.append(rightMaster.getError());
			 /*
	         sb.append(",");
	         sb.append(leftTPointStatus.activePoint.position);
	         sb.append(",");
	         sb.append(rightTPointStatus.activePoint.position);
	         */
			sb.append("\n");

			fw.write(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		maxSpeed = Math.max(maxSpeed, Math.max(Math.abs(leftMaster.getSpeed()), Math.abs(rightMaster.getSpeed())));
		SmartDashboard.putNumber("Max Speed", maxSpeed);
		SmartDashboard.putNumber("Left", leftMaster.getSpeed());
		SmartDashboard.putNumber("Right", rightMaster.getSpeed());
		SmartDashboard.putNumber("Throttle", leftMaster.nativeToRPS(leftMaster.canTalon.getSetpoint()));
		SmartDashboard.putNumber("Heading", getGyroOutput());
		SmartDashboard.putNumber("Left Setpoint", leftMaster.nativeToRPS(leftMaster.canTalon.getSetpoint()));
		SmartDashboard.putNumber("Left Error", leftMaster.nativeToRPS(leftMaster.canTalon.getError()));
		SmartDashboard.putNumber("Right Setpoint", rightMaster.nativeToRPS(rightMaster.canTalon.getSetpoint()));
		SmartDashboard.putNumber("Right Error", rightMaster.nativeToRPS(rightMaster.canTalon.getError()));
		SmartDashboard.putNumber("Left F", leftMaster.canTalon.getF());
		SmartDashboard.putNumber("Right F", rightMaster.canTalon.getF());
		SmartDashboard.putNumber("Left P", leftMaster.canTalon.getP());
		SmartDashboard.putNumber("Right P", rightMaster.canTalon.getP());
		SmartDashboard.putBoolean("In low gear?", lowGear);
	}

	public void logData(double sp) {
		try (FileWriter fw = new FileWriter(logFN, true)) {
			StringBuilder sb = new StringBuilder();
			sb.append((System.nanoTime() - startTime) / Math.pow(10, 9));
			sb.append(",");
			sb.append(leftMaster.getSpeed());
			sb.append(",");
			sb.append(rightMaster.getSpeed());
			sb.append(",");
			sb.append(leftMaster.getError());
			sb.append(",");
			sb.append(rightMaster.getError());
			sb.append(",");
			sb.append(rightTPointStatus.activePoint.position);
			sb.append(",");
			sb.append(leftTPointStatus.activePoint.velocity);
			sb.append(",");
			sb.append(rightTPointStatus.activePoint.velocity);
			sb.append("\n");

			fw.write(sb.toString());

			SmartDashboard.putNumber("Left", leftMaster.getSpeed());
			SmartDashboard.putNumber("Right", rightMaster.getSpeed());
			SmartDashboard.putNumber("Left Pos inches", leftMaster.nativeToRPS(leftMaster.canTalon.getEncPosition()) /
					10 * Math.PI * 4);
			SmartDashboard.putNumber("Right Pos inches", rightMaster.nativeToRPS(rightMaster.canTalon.getEncPosition()
			) / 10 * Math.PI * 4);
			SmartDashboard.putNumber("Right Pos", rightMaster.canTalon.getEncPosition());
			SmartDashboard.putNumber("Left Pos", leftMaster.canTalon.getEncPosition());
			SmartDashboard.putNumber("Throttle", leftMaster.nativeToRPS(leftMaster.canTalon.getSetpoint()));
			SmartDashboard.putNumber("Heading", getGyroOutput());
			SmartDashboard.putNumber("Left Setpoint", leftMaster.nativeToRPS(leftMaster.canTalon.getSetpoint()));
			SmartDashboard.putNumber("Left Error", leftMaster.nativeToRPS(leftMaster.canTalon.getError()));
			SmartDashboard.putNumber("Right Setpoint", rightMaster.nativeToRPS(rightMaster.canTalon.getSetpoint()));
			SmartDashboard.putNumber("Right Error", rightMaster.nativeToRPS(rightMaster.canTalon.getError()));
			sb.append(PID_SCALE * sp * leftMaster.getMaxSpeed());
			sb.append(",");
			sb.append(PID_SCALE * sp * rightMaster.getMaxSpeed());
	         /*
	         sb.append(",");
	         sb.append(leftTPointStatus.activePoint.position);
	         sb.append(",");
	         sb.append(rightTPointStatus.activePoint.position);
	         */
			sb.append("\n");

			fw.write(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void initDefaultCommand() {
		startTime = System.nanoTime();
		overrideNavX = false;
//		setDefaultCommand(new PIDTest(this));
//		setDefaultCommand(new OpArcadeDrive(this, oi));
//		setDefaultCommand(new DefaultArcadeDrive(straightPID, this, oi));
//		setDefaultCommand(new ExecuteProfile(this));
	}

	public double getGyroOutput() {
		return 0;
		//return navx.pidGet();
	}

	public void setLowGear(boolean setLowGear) {
		if (shifter != null) {
			if (setLowGear) {
				shifter.set(DoubleSolenoid.Value.kForward);
				rightMaster.switchToLowGear();
				leftMaster.switchToLowGear();
				lowGear = true;
			} else {
				shifter.set(DoubleSolenoid.Value.kReverse);
				rightMaster.switchToHighGear();
				leftMaster.switchToHighGear();
				lowGear = false;
			}
			timeLastShifted = System.currentTimeMillis();
		} else {
			System.out.println("You're trying to shift gears, but your drive doesn't have a shifter.");
		}
	}

	public double getLeftSpeed() {
		return leftMaster.getSpeed();
	}

	public double getRightSpeed() {
		return rightMaster.getSpeed();
	}

	public boolean inLowGear() {
		return lowGear;
	}

	public boolean shouldDownshift() {
		boolean okToShift = Math.min(Math.abs(getLeftSpeed()), Math.abs(getRightSpeed())) < downshift && !lowGear &&
				!overrideAutoShift || oi.getFwd() == 0 && oi.getRot() != 0 && !overrideAutoShift;
		if (shiftDelay != null) {
			return okToShift && (System.currentTimeMillis() - timeLastShifted > shiftDelay * 1000);
		}
		if (okToShift && !okToDownshift) {
			okToDownshift = true;
			timeBelowShift = System.currentTimeMillis();
		} else if (!okToShift && okToDownshift) {
			okToDownshift = false;
		}
		return (System.currentTimeMillis() - timeBelowShift > downTimeThresh * 1000 && okToShift);
	}

	public boolean shouldUpshift() {
		boolean okToShift = Math.max(Math.abs(getLeftSpeed()), Math.abs(getRightSpeed())) > upshift && lowGear &&
				!overrideAutoShift && Math.abs(oi.getFwd()) > upshiftFwdDeadband;
		if (shiftDelay != null) {
			return okToShift && (System.currentTimeMillis() - timeLastShifted > shiftDelay * 1000);
		}
		if (okToShift && !okToUpshift) {
			okToUpshift = true;
			timeAboveShift = System.currentTimeMillis();
		} else if (!okToShift && okToUpshift) {
			okToUpshift = false;
		}
		return (System.currentTimeMillis() - timeAboveShift > upTimeThresh * 1000 && okToShift);
	}

	public void autoShift() {
		if (shouldUpshift()) {
			setLowGear(false);
		} else if (shouldDownshift()) {
			setLowGear(true);
		}
	}

	/**
	 * Simple helper function for clipping output to the -1 to 1 scale.
	 *
	 * @param in The number to be processed.
	 * @return That number, clipped to 1 if it's greater than 1 or clipped to -1 if it's less than -1.
	 */
	private static double clipToOne(double in) {
		if (in > 1)
			return 1;
		else if (in < -1)
			return -1;
		else
			return in;
	}
}
