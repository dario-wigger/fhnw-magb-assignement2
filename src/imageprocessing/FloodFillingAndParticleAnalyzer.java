package imageprocessing;

import imageprocessing.colors.GrayscaleImage;
import org.eclipse.swt.graphics.ImageData;

import main.Picsi;
import org.eclipse.swt.graphics.RGB;
import utils.Parallel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

public class FloodFillingAndParticleAnalyzer implements IImageProcessor {

	public static int s_background = 0; // white
	public static int s_foreground = 1; // black

	@Override
	public boolean isEnabled(int imageType) {
		return true;
	}

	@Override
	public ImageData run(ImageData input, int imageType) {

		ImageData output = transformToBinaryAndApplyMorphology(input, imageType);

		List<ParticleData> particleData = floodFillAndAnalyzeParticles(output);
		System.out.println("Anzahl Mnzen: " + particleData.size());

		output = falseColor(output, particleData.size() + 2);

		for(ParticleData data : particleData) {
			data.drawBoundingBox(output);
			data.drawConvexHull(output);
			data.drawCentreOfGravity(output);
		}

		System.out.println("| Label | Area   | Center of Gravity (x,y) | Eccentricity | Perimeter | Circularity | Bounding Box (x1, y1), (x2, y2) | Convex Hull Area | Density  | Diameter | Convex Hull [(x1, y1), ...] ");
		System.out.println("|-------|--------|-------------------------|--------------|-----------|-------------|---------------------------------|------------------|----------|----------|------------------------|");

		for (ParticleData p : particleData) {
			p.printData();
		}

		return output;
	}

	/**
	 * Ensures that the image is in binary form and applies closing to the image
	 * @param input image
	 * @param imageType
	 * @return binary image
	 */
	private static ImageData transformToBinaryAndApplyMorphology(ImageData input, int imageType) {
		ImageData output = input;

		if(imageType != Picsi.IMAGE_TYPE_GRAY && imageType != Picsi.IMAGE_TYPE_BINARY) {
			output = GrayscaleImage.grayscale(input);
		}

		if(imageType != Picsi.IMAGE_TYPE_BINARY) {
			final int threshold = Binarization.otsuThreshold(input);
			output = Binarization.binarize(output, threshold, false, false);
		}

		return MorphologicFilter.closing(output, MorphologicFilter.s_diamond5, 2, 2, 1);
	}

	/**
	 * Labels a binarized grayscale image and analyzes the particles
	 * @param imageData input: grayscale image with intensities 0 and 1 only, output: labeled foreground regions
	 * @return List of particle data
	 */
	public static List<ParticleData> floodFillAndAnalyzeParticles(ImageData imageData) {
		assert ImageProcessing.determineImageType(imageData) == Picsi.IMAGE_TYPE_GRAY;

		Set<Coordinate> visited = new HashSet<>();
		List<ParticleData> particleData = new ArrayList<>();
		int label = 1;

		for(int v = 0; v < imageData.height; v ++) {
			for(int u = 0; u < imageData.width; u ++) {
				if(imageData.getPixel(u, v) == 1) {
					particleData.add(floodFillAndAnalyzeParticles(imageData, visited, u, v, ++label));
				}
			}
		}

		return particleData;
	}

	/**
	 * Finds all connected pixels, marks them with a label and analyzes the region
	 * @param imageData binarized grayscale image
	 * @param visited already visited pixels
	 * @param startX start x coordinate
	 * @param startY start y coordinate
	 * @param label value to set to the pixels of the found region
	 * @return Particle data
	 */
	public static ParticleData floodFillAndAnalyzeParticles(ImageData imageData, Set<Coordinate> visited, int startX, int startY, int label) {

		ParticleData particleData = new ParticleData(label);
		Queue<Coordinate> queue = new ArrayDeque<>();
		queue.add(new Coordinate(startX, startY));

		List<Coordinate> coordinates = new ArrayList<>();
		Set<Coordinate> perimeter = new HashSet<>();
		Coordinate mostRightPixel = new Coordinate(startX, startY);
		Coordinate mostLeftPixel = new Coordinate(startX, startY);
		Coordinate mostTopPixel = new Coordinate(startX, startY);
		Coordinate mostBottomPixel = new Coordinate(startX, startY);

		int numberOfPixels = 0;
		int sumXCoordinates = 0;
		int sumYCoordinates = 0;

		while(!queue.isEmpty()) {
			numberOfPixels ++;
			Coordinate coordinate = queue.poll();
			imageData.setPixel((int) coordinate.x(), (int) coordinate.y(), label);
			sumXCoordinates += coordinate.x();
			sumYCoordinates += coordinate.y();
			coordinates.add(coordinate);

			if(coordinate.isLeftOf(mostLeftPixel)) {
				mostLeftPixel = coordinate;
			} else if(coordinate.isRightOf(mostRightPixel)) {
				mostRightPixel = coordinate;
			}
			if(coordinate.isAbove(mostTopPixel)) {
				mostTopPixel = coordinate;
			} else if(coordinate.isBelowOf(mostBottomPixel)) {
				mostBottomPixel = coordinate;
			}

			Coordinate leftCoordinate   = new Coordinate(coordinate.x() - 1, coordinate.y());
			Coordinate rightCoordinate  = new Coordinate(coordinate.x() + 1, coordinate.y());
			Coordinate topCoordinate    = new Coordinate(coordinate.x(), coordinate.y() - 1);
			Coordinate bottomCoordinate = new Coordinate(coordinate.x(), coordinate.y() + 1);

			boolean isLeftBoundary   = !visited.contains(leftCoordinate)   && processPixel(imageData, queue, leftCoordinate);
			boolean isRightBoundary  = !visited.contains(rightCoordinate)  && processPixel(imageData, queue, rightCoordinate);
			boolean isTopBoundary    = !visited.contains(topCoordinate)    && processPixel(imageData, queue, topCoordinate);
			boolean isBottomBoundary = !visited.contains(bottomCoordinate) && processPixel(imageData, queue, bottomCoordinate);

			if (isLeftBoundary || isRightBoundary || isTopBoundary || isBottomBoundary) {
				perimeter.add(coordinate);
			}

			visited.add(leftCoordinate);
			visited.add(rightCoordinate);
			visited.add(topCoordinate);
			visited.add(bottomCoordinate);
		}

		particleData.calculateArea(numberOfPixels);
		particleData.calculateBoundingBox(mostLeftPixel, mostRightPixel, mostTopPixel, mostBottomPixel);
		particleData.calculateCenterOfGravity(sumXCoordinates, sumYCoordinates, numberOfPixels);
		particleData.calculateEccentricity(sumXCoordinates, sumYCoordinates, numberOfPixels, coordinates);
		particleData.calculatePerimeterAndCircularity(perimeter);
		particleData.calculateConvexHull(coordinates);

		return particleData;
	}

	/**
	 * Determines if a pixel belongs to a region and if the pixel is a boundary pixel
	 * Definition boundary pixel: A pixel is a boundary pixel if it does not belong to the region
	 * @param imageData binarized grayscale image
	 * @param queue queue where the pixel is added if it belongs to the region
	 * @param coordinate pixel-coordinate
	 * @return true if the pixel is a boundary pixel, false otherwise
	 */
	private static boolean processPixel(ImageData imageData, Queue<Coordinate> queue, Coordinate coordinate) {
		if (isMatch(coordinate, imageData)) {
			queue.add(coordinate);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Checks if a given coordinate is in the range of the given image and is a foreground pixel
	 *
	 * @param coordinate the coordinate to check
	 * @param imageData the image data to check against
	 * @return {@code true} if the coordinate is within the bounds of the image and the pixel value at the coordinate is 1; {@code false} otherwise
	 */
	public static boolean isMatch(Coordinate coordinate, ImageData imageData) {
		if(coordinate.x() < 0 || coordinate.x() >= imageData.width ||
				coordinate.y() < 0 || coordinate.y() >= imageData.height) {
			return false;
		}
		return imageData.getPixel((int) coordinate.x(), (int) coordinate.y()) == s_foreground;
	}

	/**
	 * False color presentation of labeled grayscale image
	 * @param inData labeled grayscale image
	 * @param n number of different false colors (<= 256)
	 * @return indexed color image
	 */
	public static ImageData falseColor(ImageData inData, int n) {
		assert ImageProcessing.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;
		assert 0 < n && n <= 256;

		var targetImage = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

		RGB[] colors = generateDistinctColors(n);

		Parallel.For(0, inData.height, v -> {
			for (int u=0; u < inData.width; u++) {
				int group = inData.getPixel(u, v);

				if(group == 0) {
					targetImage.setPixel(u, v, s_background);
				} else {
					targetImage.setPixel(u, v, targetImage.palette.getPixel(colors[group % n]));
				}
			}
		});

		return targetImage;
	}

	/**
	 * Generates an array of distinct colors.
	 *
	 * @param n the number of distinct colors to generate
	 * @return an array of {@link RGB} objects representing the distinct colors
	 */
	private static RGB[] generateDistinctColors(int n) {
		RGB[] colors = new RGB[n];
		float step = 360.0f / n;
		for (int i = 0; i < n; i++) {
			float hue = step * i;
			colors[i] = HSBtoRGB(hue, 1.0f, 1.0f);
		}
		return colors;
	}

	/**
	 * Converts HSB (hue, saturation, brightness) values to an RGB color.
	 *
	 * @param hue the hue component, in degrees (0-360)
	 * @param saturation the saturation component (0-1)
	 * @param brightness the brightness component (0-1)
	 * @return an {@link RGB} object representing the converted color
	 */
	private static RGB HSBtoRGB(float hue, float saturation, float brightness) {
		int rgb = java.awt.Color.HSBtoRGB(hue / 360, saturation, brightness);
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return new RGB(r, g, b);
	}

	private static class ParticleData {

		private final int label;
		private int area;
		private Coordinate centreOfGravity;
		private double eccentricity;
		private int perimeter;
		private double circularity;
		private BoundingBox boundingBox;
		private List<Coordinate> convexHull;
		private double convexHullArea;
		private double density;
		private double diameter;

		public ParticleData(int label) {
			this.label = label;
		}

		/**
		 * Sets the area of the particle.
		 *
		 * @param numberOfPixels the number of pixels in the particle
		 */
		public void calculateArea(int numberOfPixels) {
			this.area = numberOfPixels;
		}

		/**
		 * Calculates and sets the bounding box of the particle.
		 *
		 * @param leftPixel   the leftmost pixel of the particle
		 * @param rightPixel  the rightmost pixel of the particle
		 * @param topPixel    the topmost pixel of the particle
		 * @param bottomPixel the bottommost pixel of the particle
		 */
		public void calculateBoundingBox(Coordinate leftPixel, Coordinate rightPixel, Coordinate topPixel, Coordinate bottomPixel) {
			this.boundingBox = new BoundingBox(new Coordinate(leftPixel.x, topPixel.y), new Coordinate(rightPixel.x, bottomPixel.y));
		}

		/**
		 * Calculates and sets the center of gravity of the particle.
		 *
		 * @param sumXCoordinates the sum of x coordinates of all pixels in the particle
		 * @param sumYCoordinates the sum of y coordinates of all pixels in the particle
		 * @param numberOfPixels  the number of pixels in the particle
		 */
		public void calculateCenterOfGravity(int sumXCoordinates, int sumYCoordinates, int numberOfPixels) {
			this.centreOfGravity = new Coordinate((double) sumXCoordinates / numberOfPixels, (double) sumYCoordinates / numberOfPixels);
		}

		/**
		 * Calculates and sets the eccentricity of the particle.
		 *
		 * @param sumXCoordinates the sum of x coordinates of all pixels in the particle
		 * @param sumYCoordinates the sum of y coordinates of all pixels in the particle
		 * @param numberOfPixels  the number of pixels in the particle
		 * @param coordinates     the list of coordinates of all pixels in the particle
		 */
		public void calculateEccentricity(int sumXCoordinates, int sumYCoordinates, int numberOfPixels, List<Coordinate> coordinates) {

			if(numberOfPixels == 1) {
				this.eccentricity = 1;
			} else {
				double meanX = (double) sumXCoordinates / numberOfPixels;
				double meanY = (double) sumYCoordinates / numberOfPixels;
				double sxx = 0, syy = 0, sxy = 0;

				for (Coordinate c : coordinates) {
					double dx = c.x() - meanX;
					double dy = c.y() - meanY;
					sxx += dx * dx;
					syy += dy * dy;
					sxy += dx * dy;
				}

				sxx /= numberOfPixels;
				syy /= numberOfPixels;
				sxy /= numberOfPixels;

				double trace = sxx + syy;
				double det = sxx * syy - sxy * sxy;
				double sqrt = Math.sqrt(trace * trace - 4 * det);
				double eigenvalue1 = (trace + sqrt) / 2;
				double eigenvalue2 = (trace - sqrt) / 2;

				double majorAxis = Math.sqrt(Math.max(eigenvalue1, eigenvalue2));
				double minorAxis = Math.sqrt(Math.min(eigenvalue1, eigenvalue2));
				this.eccentricity = Math.sqrt(1 - (minorAxis * minorAxis) / (majorAxis * majorAxis));
			}
		}

		/**
		 * Calculates and sets the perimeter and circularity of the particle.
		 *
		 * @param boundaryPixels the set of boundary pixels of the particle
		 */
		public void calculatePerimeterAndCircularity(Set<Coordinate> boundaryPixels) {
			this.perimeter = boundaryPixels.size();

			if(boundaryPixels.size() == 1) {
				this.circularity = 1;
			} else {
				this.circularity = (4 * Math.PI * area) / (Math.pow(perimeter, 2));
			}
		}

		/**
		 * Calculates and sets the convex hull of the particle using a graham scan.
		 *
		 * @param coordinates the list of coordinates of all pixels in the particle
		 */
		public void calculateConvexHull(List<Coordinate> coordinates) {
			this.convexHull = grahamScan(coordinates);
			calculateConvexHullArea();
			calculateConvexHullDiameter();
			this.density = area / convexHullArea;
		}

		/**
		 * Performs the Graham scan algorithm to find the convex hull of a set of points.
		 *
		 * @param coordinates the list of coordinates of all pixels in the particle
		 * @return the list of points forming the convex hull
		 */
		private List<Coordinate> grahamScan(List<Coordinate> coordinates) {
			if (coordinates.size() <= 1) return coordinates;

			// Step 1: Find the point with the lowest y-coordinate (and the leftmost point if tie)
			Coordinate start = coordinates.stream().min(Comparator.comparing(Coordinate::y).thenComparing(Coordinate::x)).get();
			coordinates.remove(start);

			// Step 2: Sort the points by polar angle with respect to the start point
			coordinates.sort((a, b) -> {
				double angle1 = Math.atan2(a.y() - start.y(), a.x() - start.x());
				double angle2 = Math.atan2(b.y() - start.y(), b.x() - start.x());
				if (angle1 < angle2) return -1;
				if (angle1 > angle2) return 1;
				double distance1 = Math.sqrt(Math.pow(a.x() - start.x(), 2) + Math.pow(a.y() - start.y(), 2));
				double distance2 = Math.sqrt(Math.pow(b.x() - start.x(), 2) + Math.pow(b.y() - start.y(), 2));
				return Double.compare(distance1, distance2);
			});

			// Step 3: Initialize the hull with the start point and the first point
			Stack<Coordinate> hull = new Stack<>();
			hull.push(start);
			hull.push(coordinates.get(0));

			// Step 4: Process the remaining points
			for (int i = 1; i < coordinates.size(); i++) {
				Coordinate top = hull.pop();
				while (!hull.isEmpty() && ccw(hull.peek(), top, coordinates.get(i)) <= 0) {
					top = hull.pop();
				}
				hull.push(top);
				hull.push(coordinates.get(i));
			}

			return new ArrayList<>(hull);
		}

		/**
		 * Computes the counter-clockwise value for three points.
		 *
		 * @param a the first point
		 * @param b the second point
		 * @param c the third point
		 * @return a positive value if counter-clockwise, negative if clockwise, zero if collinear
		 */
		private int ccw(Coordinate a, Coordinate b, Coordinate c) {
			return Double.compare((b.x() - a.x()) * (c.y() - a.y()), (b.y() - a.y()) * (c.x() - a.x()));
		}

		/**
		 * Calculates and sets the area of the convex hull.
		 */
		private void calculateConvexHullArea() {
			if (convexHull.size() < 3) {
				this.convexHullArea = 1;
			} else {
				double area = 0.0;
				int n = convexHull.size();

				for (int i = 0; i < n; i++) {
					Coordinate current = convexHull.get(i);
					Coordinate next = convexHull.get((i + 1) % n);
					area += current.x() * next.y() - next.x() * current.y();
				}

				this.convexHullArea = Math.abs(area) / 2.0;
			}
		}

		/**
		 * Calculates and sets the diameter of the convex hull.
		 */
		private void calculateConvexHullDiameter() {
			int n = convexHull.size();
			if (n < 2) {
				this.diameter = 0;
			} else {
				int k = 1;
				double maxDistance = 0.0;

				for (int i = 0; i < n; i++) {
					while (true) {
						double currentDistance = squaredDistance(convexHull.get(i), convexHull.get((k + 1) % n));
						double nextDistance = squaredDistance(convexHull.get(i), convexHull.get(k));

						if (currentDistance > nextDistance) {
							k = (k + 1) % n;
						} else {
							break;
						}
					}

					double distance = squaredDistance(convexHull.get(i), convexHull.get(k));
					if (distance > maxDistance) {
						maxDistance = distance;
					}
				}

				this.diameter = Math.sqrt(maxDistance);
			}
		}

		/**
		 * Computes the squared distance between two coordinates.
		 *
		 * @param a the first coordinate
		 * @param b the second coordinate
		 * @return the squared distance between the coordinates
		 */
		public double squaredDistance(Coordinate a, Coordinate b) {
			double dx = a.x() - b.x();
			double dy = a.y() - b.y();
			return dx * dx + dy * dy;
		}

		/**
		 * Draws the bounding box on the given image.
		 *
		 * @param image the image on which to draw the bounding box
		 */
		public void drawBoundingBox(ImageData image) {
			Coordinate topRight = new Coordinate(boundingBox.b.x, boundingBox.a.y);
			Coordinate topLeft = boundingBox.a;
			Coordinate bottomLeft = new Coordinate(boundingBox.a.x, boundingBox.b.y);
			Coordinate bottomRight = boundingBox.b;

			drawLine(image, topLeft, topRight, 0xFF0000);
			drawLine(image, bottomLeft, bottomRight, 0xFF0000);
			drawLine(image, topLeft, bottomLeft, 0xFF0000);
			drawLine(image, topRight, bottomRight, 0xFF0000);
		}

		/**
		 * Draws the convex hull on the given image.
		 *
		 * @param image the image on which to draw the convex hull
		 */
		public void drawConvexHull(ImageData image) {
			for (int i = 0; i < convexHull.size(); i++) {
				Coordinate start = convexHull.get(i);
				Coordinate end = convexHull.get((i + 1) % convexHull.size());
				drawLine(image, start, end, 0x00FF00);
			}
		}

		/**
		 * Draws the center of gravity on the given image.
		 *
		 * @param image the image on which to draw the center of gravity
		 */
		public void drawCentreOfGravity(ImageData image) {
			for(int x = (int) (centreOfGravity.x - 1); x <= centreOfGravity.x + 1; x ++) {
				for(int y = (int) (centreOfGravity.y - 1); y <= centreOfGravity.y + 1; y ++) {
					image.setPixel(x, y, 0xFFFFFF);
				}
			}
		}

		/**
		 * Draws a line on the given image.
		 *
		 * @param image the image on which to draw the line
		 * @param start the starting coordinate of the line
		 * @param end   the ending coordinate of the line
		 * @param color the color of the line
		 */
		private void drawLine(ImageData image, Coordinate start, Coordinate end, int color) {
			int dx = (int) Math.abs(end.x - start.x);
			int dy = (int) Math.abs(end.y - start.y);
			int sx = start.x < end.x ? 1 : -1;
			int sy = start.y < end.y ? 1 : -1;
			int err = dx - dy;
			int x = (int) start.x;
			int y = (int) start.y;

			while (true) {
				image.setPixel(x, y, color);

				if (x == end.x && y == end.y) break;

				int e2 = 2 * err;
				if (e2 > -dy) {
					err -= dy;
					x += sx;
				}
				if (e2 < dx) {
					err += dx;
					y += sy;
				}
			}
		}

		/**
		 * Prints the data of the particle in a formatted table row.
		 */
		public void printData() {
			StringBuilder convexHullString = new StringBuilder();
			for (Coordinate coord : convexHull) {
				convexHullString.append(String.format("(%.2f, %.2f), ", coord.x(), coord.y()));
			}

			if (convexHullString.length() > 0) {
				convexHullString.setLength(convexHullString.length() - 2);
			}

			System.out.printf(
				"| %-5d | %6d |        (%6.2f, %6.2f) | %.4f       | %-9d | %.4f      | (%3.0f, %3.0f), (%3.0f, %3.0f)          | %-16.2f | %.4f   | %6.2f   | [%s]  |%n",
				label,
				area,
				centreOfGravity.x(), centreOfGravity.y(),
				eccentricity,
				perimeter,
				circularity,
				boundingBox.a.x(), boundingBox.a.y(), boundingBox.b.x(), boundingBox.b.y(),
				convexHullArea,
				density,
				diameter,
				convexHullString
			);
		}
	}

	/**
	 * Represents a coordinate with x and y values.
	 *
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 */
	private record Coordinate(double x, double y) {

		/**
		 * Checks if this coordinate is to the right of the specified coordinate.
		 *
		 * @param other the other coordinate
		 * @return {@code true} if this coordinate is to the right of the other coordinate; {@code false} otherwise
		 */
		public boolean isRightOf(Coordinate other) {
			return this.x > other.x;
		}

		/**
		 * Checks if this coordinate is to the left of the specified coordinate.
		 *
		 * @param other the other coordinate
		 * @return {@code true} if this coordinate is to the left of the other coordinate; {@code false} otherwise
		 */
		public boolean isLeftOf(Coordinate other) {
			return this.x < other.x;
		}

		/**
		 * Checks if this coordinate is below the specified coordinate.
		 *
		 * @param other the other coordinate
		 * @return {@code true} if this coordinate is below the other coordinate; {@code false} otherwise
		 */
		public boolean isBelowOf(Coordinate other) {
			return this.y > other.y;
		}

		/**
		 * Checks if this coordinate is above the specified coordinate.
		 *
		 * @param other the other coordinate
		 * @return {@code true} if this coordinate is above the other coordinate; {@code false} otherwise
		 */
		public boolean isAbove(Coordinate other) {
			return this.y < other.y;
		}

		/**
		 * Indicates whether some other object is "equal to" this one.
		 * Two coordinates are equal if they have the same x and y values.
		 *
		 * @param obj the reference object with which to compare
		 * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}

			Coordinate that = (Coordinate) obj;

			if (x != that.x) {
				return false;
			}
			return y == that.y;
		}

		@Override
		public String toString() {
			return String.format("(%.2f, %.2f)", x, y);
		}
	}

	/**
	 * Represents a bounding box defined by two coordinates.
	 *
	 * @param a the first coordinate
	 * @param b the second coordinate
	 */
	private record BoundingBox(Coordinate a, Coordinate b) {

		@Override
		public String toString() {
			return a + ", " + b;
		}
	}
}
