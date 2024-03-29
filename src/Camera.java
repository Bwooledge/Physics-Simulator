import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Canvas;
import java.awt.Font;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;

public class Camera {
	//position and orientation of camera
	private double[] position = new double[] {0, 0, 0};
	private double[] normal_vector = new double[] {0, 0, 1};
	private double[] right_vector = new double[] {1, 0, 0};
	private double[] up_vector = new double[] {0, 1, 0};
		
	//fov and clipping distance
	private double fov = 90 * Math.PI / 180;
	private double fov_slope;
	private int clipping_distance;
	private int maximum_distance = 10000;
	
	//movement of camera
	private int[] movement = new int[3];
	private int[] rotation = new int[2];
	private double[] move_speeds = new double[3];
	private double[] rotate_speeds = new double[2];
	private double max_move_speed = 4;
	private double[] max_rotate_speeds = new double[] {0.03, 0.025};
	private double speed_multiplier = 1;
	private double move_acceleration_rate = 0.8;
	private double rotate_acceleration_rate = 0.004;
	
	public Camera()
	{
		//clipping_dist and fov
		int w = GraphicsRunner.WIDTH;
		clipping_distance = (int)(w * 0.5 / Math.tan(fov / 2));
		fov_slope = w / clipping_distance;
	}
	
	public Camera(double[] position, double[] orientation)
	{
		//clipping_dist and fov
		int w = GraphicsRunner.WIDTH;
		clipping_distance = (int)(w * 0.5 / Math.tan(fov / 2));
		fov_slope = w / clipping_distance;
		
		//adjust position
		this.position = position;
		
		//rotate to orientation
		double theta1 = orientation[0] * Math.PI / 180;
		double theta2 = orientation[1] * Math.PI / 180;
		normal_vector[0] = Math.cos(theta1) * Math.cos(theta2);
		normal_vector[1] = Math.sin(theta2);
		normal_vector[2] = Math.sin(theta1) * Math.cos(theta2);
		right_vector[0] = Math.cos(theta1 - Math.PI/2);
		right_vector[2] = Math.sin(theta1 - Math.PI/2);
		up_vector[0] = Math.cos(theta1) * Math.cos(theta2 + Math.PI/2);
		up_vector[1] = Math.sin(theta2 + Math.PI/2);
		up_vector[2] = Math.sin(theta1) * Math.cos(theta2 + Math.PI/2);
	}
	
	public double getFov()
	{
		return fov;
	}
	
	public double getSlope()
	{
		return fov_slope;
	}
	
	public double[] getPosition()
	{
		return position;
	}
	
	public double[] getNormal()
	{
		return normal_vector;
	}
	
	public double[] getRight()
	{
		return right_vector;
	}
	
	public double[] getUp()
	{
		return up_vector;
	}
	
	public int getClippingDistance()
	{
		return clipping_distance;
	}
	
	public int getMaximumDistance()
	{
		return maximum_distance;
	}
	
	public int getMovement(int i)
	{
		return movement[i];
	}
	
	public int getRotation(int i)
	{
		return rotation[i];
	}
	
	public void rotateCam()
	{
		//acceleration of camera
		double length = Math.sqrt(Math.pow(rotation[0], 2) + Math.pow(rotation[1], 2));
		for(int i = 0; i < rotation.length; i++)
		{
			if(rotation[i] == 0 && rotate_speeds[i] != 0)
				if(Math.abs(rotate_speeds[i]) < rotate_acceleration_rate)
					rotate_speeds[i] = 0;
				else
					rotate_speeds[i] -= rotate_acceleration_rate * Math.abs(rotate_speeds[i]) / rotate_speeds[i];
			else if(rotation[i] < 0 && rotate_speeds[i] > -max_rotate_speeds[i] || rotation[i] > 0 && rotate_speeds[i] < max_rotate_speeds[i])
				rotate_speeds[i] += rotate_acceleration_rate * rotation[i] / length;
		}
		
		if(rotate_speeds[0] != 0)
		{
			//get angle of normal vector on xz plane
			double theta1 = Math.atan2(normal_vector[2], normal_vector[0]);
			
			//change angle
			theta1 -= rotate_speeds[0] * Simulation.BASE_FRAME_RATE / Simulation.FRAME_RATE;
			
			//adjust normal vector
			double c = Math.sqrt(Math.pow(normal_vector[0], 2) + Math.pow(normal_vector[2], 2));
			normal_vector[0] = c * Math.cos(theta1);
			normal_vector[2] = c * Math.sin(theta1);
			
			//adjust up vector
			double d = Math.sqrt(Math.pow(up_vector[0], 2) + Math.pow(up_vector[2], 2));
			double theta3 = Math.atan2(up_vector[2], up_vector[0]) - rotate_speeds[0] * Simulation.BASE_FRAME_RATE / Simulation.FRAME_RATE;
			up_vector[0] = d * Math.cos(theta3);
			up_vector[2] = d * Math.sin(theta3);
					
			//adjust right vector
			double e = Math.sqrt(Math.pow(right_vector[0], 2) + Math.pow(right_vector[2], 2));
			right_vector[0] = e * Math.cos(theta1 - Math.PI/2);
			right_vector[2] = e * Math.sin(theta1 - Math.PI/2);
		}
		if(rotate_speeds[1] != 0)
		{
			//get angle of normal vector on xz plane and on the plane generated by the normal and up vectors
			double theta1 = Math.atan2(normal_vector[2], normal_vector[0]);
			double c = Math.sqrt(Math.pow(normal_vector[0], 2) + Math.pow(normal_vector[2], 2));
			double theta2 = Math.atan2(normal_vector[1], c);
			
			//change angle
			theta2 += rotate_speeds[1] * Simulation.BASE_FRAME_RATE / Simulation.FRAME_RATE;
			if(theta2 > Math.PI / 2) theta2 = Math.PI / 2;
			else if(theta2 < -Math.PI / 2) theta2 = -Math.PI / 2;
			
			//adjust normal vector
			normal_vector[1] = Math.sin(theta2);
			c = Math.cos(theta2);
			normal_vector[0] = c * Math.cos(theta1);
			normal_vector[2] = c * Math.sin(theta1);
			
			//adjust up vector
			up_vector[1] = Math.sin(theta2 + Math.PI/2);
			double d = Math.cos(theta2 + Math.PI/2);
			up_vector[0] = d * Math.cos(theta1);
			up_vector[2] = d * Math.sin(theta1);
		}
	}
	
	public void moveCam()
	{
		//handle camera acceleration
		for(int i = 0; i < movement.length; i++)
		{
			if(movement[i] == 0 && move_speeds[i] != 0)
				if(Math.abs(move_speeds[i]) < move_acceleration_rate)
					move_speeds[i] = 0;
				else
					move_speeds[i] -= move_acceleration_rate * Math.abs(move_speeds[i]) / move_speeds[i];
			else if(movement[i] < 0 && move_speeds[i] > -max_move_speed || movement[i] > 0 && move_speeds[i] < max_move_speed)
				move_speeds[i] += move_acceleration_rate * movement[i];
		}
		
		//get xz plane angles of normal and right vector 
		//NOTE: up and down is kept along the y axis, but xz is based on camera orientation
		double theta = Math.atan2(normal_vector[2], normal_vector[0]);
		double theta2 = theta - Math.PI/2;
				
		//move camera according to its input
		position[0] += (Math.cos(theta2) * move_speeds[0] + Math.cos(theta) * move_speeds[2]) * speed_multiplier * Simulation.BASE_FRAME_RATE / Simulation.FRAME_RATE;
		position[1] += move_speeds[1] * speed_multiplier * Simulation.BASE_FRAME_RATE / Simulation.FRAME_RATE;
		position[2] += (Math.sin(theta2) * move_speeds[0] + Math.sin(theta) * move_speeds[2]) * speed_multiplier * Simulation.BASE_FRAME_RATE / Simulation.FRAME_RATE;
	}
	
	public void setMovement(int i, int v)
	{
		movement[i] = v;
	}
	
	public void setRotation(int i, int v)
	{
		rotation[i] = v;
	}
	
	public void setMultiplier(double val) {
		speed_multiplier = val;
	}
}
