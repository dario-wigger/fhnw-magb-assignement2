package imageprocessing;

import org.eclipse.swt.graphics.ImageData;

import main.Picsi;
import utils.Parallel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Flood Filling
 * @author Christoph Stamm
 *
 */
public class FloodFilling implements IImageProcessor {
	public static int s_background = 0; // white
	public static int s_foreground = 1; // black

	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_GRAY;
	}

	@Override
	public ImageData run(ImageData input, int imageType) {
		final int threshold = Binarization.otsuThreshold(input); // TODO: sinnvollen Schwellwert festlegen
		ImageData grayData = input;

		ImageData binaryData = Binarization.binarize(grayData, threshold, false, false);

		int nLabels = floodFill(binaryData);
		System.out.println("Anzahl Mnzen: " + nLabels);

		return falseColor(binaryData, nLabels + 2);
	}

	/**
	 * Labeling of a binarized grayscale image
	 * @param imageData input: grayscale image with intensities 0 and 1 only, output: labeled foreground regions
	 * @return number of regions
	 */
	public static int floodFill(ImageData imageData) {
		assert ImageProcessing.determineImageType(imageData) == Picsi.IMAGE_TYPE_GRAY;

		Set<Coordinate> visited = new HashSet<>();
		List<ParticleData> particleData = new ArrayList<>();
		int label = 1;

		for(int v = 0; v < imageData.height; v ++) {
			for(int u = 0; u < imageData.width; u ++) {
				if(imageData.getPixel(u, v) == 1) {
					particleData.add(floodFill(imageData, visited, u, v, ++label));
				}
			}
		}

		String format = "%-6s %-6s %-20s %-12s %-10s %-12s %-40s %-40s %-10s %-10s %-10s%n";
		System.out.printf(format, "Label", "Area", "Centre of Gravity", "Eccentricity", "Perimeter", "Circularity",
			"BoundingBox", "ConvexHull", "Convexity", "Density", "Diameter");

		for (ParticleData p : particleData) {
			System.out.printf(format, p.getLabel(), p.getArea(), p.getCentreOfGravity(), p.getEccentricity(),
				p.getPerimeter(), p.getCircularity(), p.getBoundingBox(), p.getConvexHull(), p.getConvexity(),
				p.getDensity(), p.getDiameter());
		}

		return label - 1;
	}

	public static ParticleData floodFill(ImageData imageData, Set<Coordinate> visited, int startX, int startY, int label) {

		ParticleData particleData = new ParticleData(label);
		Queue<Coordinate> queue = new ArrayDeque<>();
		queue.add(new Coordinate(startX, startY));

		List<Coordinate> coordinates = new ArrayList<>();
		Coordinate mostRightPixel = new Coordinate(startX, startY);
		Coordinate mostLeftPixel = new Coordinate(startX, startY);
		Coordinate mostTopPixel = new Coordinate(startX, startY);
		Coordinate mostBottomPixel = new Coordinate(startX, startY);

		int numberOfPixels = 0;
		int sumXCoordinates = 0;
		int sumYCoordinates = 0;
		int perimeter = 0;

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

			boolean isLeftMatch = isMatch(leftCoordinate, imageData);
			boolean isRightMatch = isMatch(rightCoordinate, imageData);
			boolean isTopMatch = isMatch(topCoordinate, imageData);
			boolean isBottomMatch = isMatch(bottomCoordinate, imageData);

			if(!visited.contains(leftCoordinate) && isLeftMatch) {
				queue.add(leftCoordinate);
			}
			if(!visited.contains(rightCoordinate) && isRightMatch) {
				queue.add(rightCoordinate);
			}
			if(!visited.contains(topCoordinate) && isTopMatch) {
				queue.add(topCoordinate);
			}
			if(!visited.contains(bottomCoordinate) && isBottomMatch) {
				queue.add(bottomCoordinate);
			}

			if(!isLeftMatch || !isRightMatch || !isTopMatch || !isBottomMatch) {
				perimeter ++;
			}

			visited.add(leftCoordinate);
			visited.add(rightCoordinate);
			visited.add(topCoordinate);
			visited.add(bottomCoordinate);
		}

		particleData.setArea(numberOfPixels);
		particleData.setBoundingBox(new BoundingBox(new Coordinate(mostLeftPixel.x, mostTopPixel.y), new Coordinate(mostRightPixel.x, mostBottomPixel.y)));
		particleData.setCentreOfGravity(new Coordinate((double) sumXCoordinates / numberOfPixels, (double) sumYCoordinates / numberOfPixels));
		particleData.setEccentricity(getEccentricity(coordinates, numberOfPixels, sumXCoordinates, sumYCoordinates));
		particleData.setPerimeter(perimeter);

		return particleData;
	}

	private static double getEccentricity(List<Coordinate> coordinates, int numberOfPixels, double sumXCoordinates, double sumYCoordinates) {

		double meanX = sumXCoordinates / numberOfPixels;
		double meanY = sumYCoordinates / numberOfPixels;
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
		double eigenvalue1 = (trace + Math.sqrt(trace * trace - 4 * det)) / 2;
		double eigenvalue2 = (trace - Math.sqrt(trace * trace - 4 * det)) / 2;

		double majorAxis = Math.sqrt(Math.max(eigenvalue1, eigenvalue2));
		double minorAxis = Math.sqrt(Math.min(eigenvalue1, eigenvalue2));
		return Math.sqrt(1 - (minorAxis * minorAxis) / (majorAxis * majorAxis));
	}

	public static boolean isMatch(Coordinate coordinate, ImageData imageData) {
		if(coordinate.x() < 0 || coordinate.x() >= imageData.width ||
				coordinate.y() < 0 || coordinate.y() >= imageData.height) {
			return false;
		}
		return imageData.getPixel((int) coordinate.x(), (int) coordinate.y()) == 1;
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

		Parallel.For(0, inData.height, v -> {
			for (int u=0; u < inData.width; u++) {
				int group = inData.getPixel(u, v);

				if(group == 0) {
					inData.setPixel(u, v, s_background);
				} else {
					inData.setPixel(u, v, (255 / n) * group);
				}
			}
		});

		return inData;
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
		private double convexity;
		private double density;
		private double diameter;

		public ParticleData(int label) {
			this.label = label;
		}

		public int getLabel() {
			return label;
		}

		public int getArea() {
			return area;
		}

		public void setArea(int area) {
			this.area = area;
		}

		public Coordinate getCentreOfGravity() {
			return centreOfGravity;
		}

		public void setCentreOfGravity(Coordinate centreOfGravity) {
			this.centreOfGravity = centreOfGravity;
		}

		public double getEccentricity() {
			return eccentricity;
		}

		public void setEccentricity(double eccentricity) {
			this.eccentricity = eccentricity;
		}

		public double getPerimeter() {
			return perimeter;
		}

		public void setPerimeter(int perimeter) {
			this.perimeter = perimeter;
		}

		public double getCircularity() {
			return circularity;
		}

		public void setCircularity(double circularity) {
			this.circularity = circularity;
		}

		public BoundingBox getBoundingBox() {
			return boundingBox;
		}

		public void setBoundingBox(BoundingBox boundingBox) {
			this.boundingBox = boundingBox;
		}

		public List<Coordinate> getConvexHull() {
			return convexHull;
		}

		public void setConvexHull(List<Coordinate> convexHull) {
			this.convexHull = convexHull;
		}

		public double getConvexity() {
			return convexity;
		}

		public void setConvexity(double convexity) {
			this.convexity = convexity;
		}

		public double getDensity() {
			return density;
		}

		public void setDensity(double density) {
			this.density = density;
		}

		public double getDiameter() {
			return diameter;
		}

		public void setDiameter(double diameter) {
			this.diameter = diameter;
		}
	}

	private record Coordinate(double x, double y) {

		public boolean isRightOf(Coordinate other) {
			return this.x > other.x;
		}

		public boolean isLeftOf(Coordinate other) {
			return this.x < other.x;
		}

		public boolean isBelowOf(Coordinate other) {
			return this.y > other.y;
		}

		public boolean isAbove(Coordinate other) {
			return this.y < other.y;
		}

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

	private record BoundingBox(Coordinate a, Coordinate b) {

		@Override
		public String toString() {
			return a + ", " + b;
		}
	}
}
