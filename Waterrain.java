import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;

public class Waterrain extends JPanel implements ActionListener, MouseListener, KeyListener {
	public static final long serialVersionUID = 1;
	private int width = 512, height = width, pixelSize = 4, slope = 2, view = 0;
	private int gridWidth = (int) (width / pixelSize), gridHeight = (int) (height / pixelSize), clickPointX = (int) (gridWidth / 2), clickPointY = (int) (gridHeight / 2);
	private double sumWater = 0, flowScaler = 0.2;
	private double[][] terrain = new double[gridWidth][gridHeight], waterTable = new double[gridWidth][gridHeight], surfaceWater = new double[gridWidth][gridHeight];
	private double[][][] transferRates = new double[gridWidth][gridHeight][10];
	private final String[] views = {"Terrain", "Water Table", "Springs", "Flowing!"};
	private final Timer t = new Timer(20, this);

	public Waterrain() {
		setLayout(null);
		addMouseListener(this);
		addKeyListener(this);
		setPreferredSize(new java.awt.Dimension(width + 128, height + 128));
		makeNewHeightMap();
		terrain = normalise(terrain, 255.0);
		t.start();
	}
	public void actionPerformed(ActionEvent e) {
		repaint();
		t.stop();
	}
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		requestFocus();
		setBackground(Color.BLACK);
//		System.out.println("Drawing view: " + view);
		switch (view) {
			case 0:
				g.drawImage(makeImage(terrain), 0, 0, this);
			break;
			case 1:
				g.drawImage(makeImage(waterTable), 0, 0, this);
			break;
			case 2:
				g.drawImage(makeImage(terrain), 0, 0, this);
			break;
			case 3:
				g.drawImage(makeImage(normalise(surfaceWater, 255.0)), 0, 0, this);
			break;
		}
		g.setColor(Color.GRAY);
		for (int i = 0; i < gridWidth; i++) {
			g.fillRect(i * pixelSize, height + 128 - (int) (0.5 * terrain[i][clickPointY]), pixelSize, height + 128);
			g.fillRect(width + 128 - (int) (0.5 * terrain[clickPointX][i]), i * pixelSize, width + 128, pixelSize);
		}
		if (waterTable[(int) (0.5 * gridWidth)][(int) (0.5 * gridHeight)] != 0) {
			g.setColor(Color.BLUE);
			for (int i = 0; i < gridWidth; i++) {
				g.drawRect(i * pixelSize, height + 128 - (int) (0.5 * waterTable[i][clickPointY]), pixelSize, height + 128);
				g.drawRect(width + 128 - (int) (0.5 * waterTable[clickPointX][i]), i * pixelSize, width + 128, pixelSize);
			}
			g.setColor(Color.CYAN);
			for (int i = 0; i < gridWidth; i++) {
				g.drawRect(i * pixelSize, height + 128 - (int) (pixelSize * surfaceWater[i][clickPointY] + 0.5 * terrain[i][clickPointY]), pixelSize, (int) (pixelSize * surfaceWater[i][clickPointY]));
				g.drawRect(width + 128 - (int) (pixelSize * surfaceWater[clickPointX][i] + 0.5 * terrain[clickPointX][i]), i * pixelSize, (int) (pixelSize * surfaceWater[clickPointX][i]), pixelSize);
			}
		}
		g.setColor(Color.RED);
		g.drawRect(pixelSize * clickPointX, 0, pixelSize, height + 127);
		g.drawRect(0, pixelSize * clickPointY, width + 127, pixelSize);
		g.drawString("" + terrain[clickPointX][clickPointY], pixelSize * (clickPointX + 2), pixelSize * (clickPointY - 1));
		g.drawString(views[view], width + 2, height + 14);
		g.drawString("" + (int) sumWater + " p3 of water.", width + 2, height + 34);
	}
	private Image makeImage(double[][] input) {
		int[] imagePixels = new int [width * height];
		int index = 0;
		int lum = 0;
		int blue = 0;
		double maxWater = 0.0;
		sumWater = 0.0;
		for (int y = 0; y < gridHeight; y++)
			for (int x = 0; x < gridWidth; x++) {
				sumWater += surfaceWater[x][y];
				if (surfaceWater[x][y] > maxWater)
					maxWater = surfaceWater[x][y];
			}
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++) {
				lum =  (int) (input[x / pixelSize][y / pixelSize]);
				if (view == 2) {
					blue = (int) waterTable[x / pixelSize][y / pixelSize];
					if (waterTable[x / pixelSize][y / pixelSize] > lum)	//Highights areas of high concavity (springs)
						lum =  15;
				} else
					blue = (int) (255.0 * surfaceWater[x / pixelSize][y / pixelSize] / maxWater);
				imagePixels[index++] = ((0xff<<24) | (lum<<16) | (lum<<8) | blue);
			}
		Image img = createImage (new MemoryImageSource(width, height, imagePixels, 0, width));
		MediaTracker mt = new MediaTracker(this);
		mt.addImage(img, 0);
		try { mt.waitForAll(); }
		catch (InterruptedException e) {}
		return img;
	}
	private void makeNewHeightMap() {
		for (int y = 0; y < gridHeight; y++)				//Initial conditions
			for (int x = 0; x < gridWidth; x++) {
				terrain[x][y] = -1;
				waterTable[x][y] = 0;
				for (int i = 0; i < 9; i++)
					transferRates[x][y][i] = 0;
				surfaceWater[x][y] = 1.0;
			}
		generateTerrain((int) (0.5 * gridWidth), (int) (0.5 * gridHeight), 255.0);//Begins the iteration
	}
	private double[][] normalise(double[][] input, double sizeness) {//scales any int array to fit in the colour range
		double gridMax = input[0][0];
		double gridMin = input[0][0];
		for (int x = 0; x < input.length; x++)
			for (int y = 0; y < input[0].length; y++) {
				if (input[x][y] > gridMax)
					gridMax = input[x][y];
				if (input[x][y] < gridMin)
					gridMin = input[x][y];
			}
		double heightScale = Math.max(gridMax - gridMin, 1);
		System.out.println("gridmax: " + gridMax + " gridMin: " + gridMin + " heightScale: " + heightScale);
		for (int y = 0; y < input.length; y++)
			for (int x = 0; x < input[0].length; x++)
				input[x][y] = sizeness * (input[x][y] - gridMin) / heightScale; 
		return input;
	}
	public void generateTerrain(int x, int y, double h) {	//Creates terrain to work on
		if (terrain[x][y] == -1) {	//It tracks down in random directions until it can't keep going
			terrain[x][y] =  h;		//then it's earliest sibling branch begins the same.
			int[] order = new int[4];
			for (int i = 0; i < 4; i++)
				order[i] = -1;
			int test;
			for (int i = 0; i < 4; i++) {
				test = (int) (4 * Math.random());
				while(search(order, test)) {
					test = (int) (4 * Math.random());
				}
				order[i] = test;
				switch (test) {
					case 0:
						if (x > 0)
							generateTerrain(x - 1, y, h - Math.random() * slope);
					break;
					case 1:
						if (x < terrain.length - 1)
							generateTerrain(x + 1, y, h - Math.random() * slope);
					break;
					case 2:
						if (y > 0)
							generateTerrain(x, y - 1, h - Math.random() * slope);
					break;
					case 3:	
						if (y < terrain[0].length - 1)
							generateTerrain(x, y + 1, h - Math.random() * slope);
					break;
				}
			}
		}
	}
	private double[][] smooth(double[][] input, int radius) {	//Applies a blur of radius to a 2d array 
		double[][] output = new double[input.length][input[0].length];
		double[][] scaler = new double[2 * radius + 1][2 * radius + 1];
		int di, dj;
		for (int i = 0; i <= 2 * radius; i++)
			for (int j = 0; j <= 2 * radius; j++) {
				di = i - radius;
				dj = j - radius;
				scaler[i][j] = (radius - Math.sqrt(di * di + dj * dj)) / radius;
				if (scaler[i][j] < 0)
					scaler[i][j] = 0;
			}
		for (int x = 0; x < input.length; x++)
			for (int y = 0; y < input[0].length; y++)
				for (int dx = -radius; dx < radius; dx++)
					for (int dy = -radius; dy < radius; dy++) {
						if (x + dx >= 0 && x + dx < input.length && y + dy >= 0 && y + dy < input[0].length)
							output[x][y] += input[x + dx][y + dy] * scaler[dx + radius][dy + radius];
						else 
							output[x][y] += input[x][y] * scaler[dx + radius][dy + radius];
					}
		return output;
	}
	private void calculateTransferRates() {	//Scales surface water flow directions by slope angle
		double diff;
		for (int x = 0; x < gridWidth; x++)
			for (int y = 0; y < gridHeight; y++) {
				transferRates[x][y][9] = 0;
				for (int dx = -1; dx <= 1; dx++)
					for (int dy = -1; dy <= 1; dy++)
						if (x + dx >= 0 && x + dx < gridWidth && y + dy >= 0 && y + dy < gridHeight) {
							diff = terrain[x][y] - terrain[x + dx][y + dy];
							if (diff > 0.0) {
								transferRates[x][y][dx + 1 + 3 * (dy + 1)] += diff;
								transferRates[x][y][9] += diff;
							} else { //mirror the negative values for moar accuracy
								transferRates[x][y][8 - (dx + 1 + 3 * (dy + 1))] += -diff;
								transferRates[x][y][9] += -diff;
							}
						}
			}
		for (int x = 0; x < gridWidth; x++)
			for (int y = 0; y < gridHeight; y++)
				if (transferRates[x][y][9] == 0) {
					transferRates[x][y][4] = 1.0;		//findNearestDown(x, y, 3);
					System.out.println("Flat pixel!");
				}
	}
/*	private boolean findNearestDown(int xHome, int yHome, int radius) {	//Tries to find a gradient for a flat area after all slopes have been found
		System.out.println("findingNearestDown to: " + xHome + ", " + yHome); 	//Abandoned WIP, not needed at this scale with doubles all around
	//	int temp = 4;//(int) ((8 * Math.random() + 5) % 9);	//Doesn't generate 4s
	//	transferRates[x][y][temp] = 1.0;
		int dx, dy;
		double[] rates = new double[10];
		for (int x = 0; x < transferRates.length; x++)
			for (int y = 0; y < transferRates[0].length; y++)
				for (int i = 0; i < 4 * (radius - 1); i++) {
					dx = i < radius ? i : (i < 2 * radius ? radius - 1 : (i < 3 * radius ? 3 * radius - i : 0));
					dy = i < radius ? 0 : (i < 2 * radius ? i - radius : (i < 3 * radius ? radius - 1 : 4 * (radius - 1) - i ));
					System.out.println("i:" + i + " dx:" + dx + " dy:" + dy);
					if (x + dx >= 0 && x + dx < transferRates.length && y + dy >= 0 && y + dy < transferRates[0].length)
						for (int j = 0; j < 9; j++) {
							if (j == 4)
								rates[i] = 0.0;
							else
								rates[i] += transferRates[x + dx][y + dy][j];
						}
				}
		return true;
	}*/
	private void letItFlow() {	//Moves surface water as stipulated by transferRates
		double[][] output = new double[gridWidth][gridHeight];
		double localScaler;
		for (int x = 1; x < gridWidth - 1; x++)	//skipping water flowing from edges
			for (int y = 1; y < gridHeight - 1; y++) {
				localScaler = 1.0 / (1.0 / flowScaler + 1.0 / (transferRates[x][y][9] / gridWidth));	//Flows slower on low relief
				for (int i = 0; i < 9; i++) {
					if (i == 4) {	//This pixel, there's no flow if it's deemed flat.
						if (transferRates[x][y][4] == 1.0) {
							output[x][y] = surfaceWater[x][y];
							break;
						} else
							output[x][y] = (1.0 - localScaler) * surfaceWater[x][y];
						continue;
					}
					output[x + i % 3 - 1][y + (int) (i / 3) - 1] += transferRates[x][y][i] * surfaceWater[x][y] * localScaler / transferRates[x][y][9];
				}
			}
		for (int x = 0; x < gridWidth; x++)
			for (int y = 0; y < gridHeight; y++)
				surfaceWater[x][y] = output[x][y];
//		surfaceWater = smooth(output, 1);
	}
	private boolean search(int[] source, int key) {
		for (int i = 0; i < source.length; i++)
			if (source[i] == key)
				return true;
		return false;
	}
	public void mousePressed(MouseEvent e) {
		requestFocus();
		if (e.getButton() == MouseEvent.BUTTON3)
			view = (view + 1) % views.length;
		else {
			if (e.getX() < width)
				clickPointX = (int) (e.getX() / pixelSize);
			if (e.getY() < height)
				clickPointY = (int) (e.getY() / pixelSize);
			System.out.println("rates: " + transferRates[clickPointX][clickPointY][0] + " " + transferRates[clickPointX][clickPointY][1] + " " + transferRates[clickPointX][clickPointY][2] + " " + transferRates[clickPointX][clickPointY][3] + " " + transferRates[clickPointX][clickPointY][4] + " " + transferRates[clickPointX][clickPointY][5] + " " + transferRates[clickPointX][clickPointY][6] + " " + transferRates[clickPointX][clickPointY][7] + " " + transferRates[clickPointX][clickPointY][8]);
			double sumRates = 0.0;
			for (int i = 0; i < 9; i++)
				sumRates += transferRates[clickPointX][clickPointY][i];
			System.out.println("surface water: " + surfaceWater[clickPointX][clickPointY] + " null rate: " + transferRates[clickPointX][clickPointY][4] + " sum rates: " + sumRates + " scaled:" + (sumRates / transferRates[clickPointX][clickPointY][9]));
		}
		t.start();
	}
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_ENTER:
				System.out.println("Generating new terrain.");
				makeNewHeightMap();
				terrain = normalise(terrain, 255);
				view = 0;
				t.start();
			break;
			case KeyEvent.VK_SPACE:
				System.out.println("Preparing selected terrain.");
				terrain = smooth(terrain, (int) (0.5 * Math.sqrt(width / pixelSize)));
				terrain = normalise(terrain, 255);
				waterTable = smooth(terrain, (int) (Math.sqrt(width / pixelSize)));
				waterTable = normalise(waterTable, 191);
				calculateTransferRates();
				view = 0;
				t.start();
			break;
			case KeyEvent.VK_PAGE_DOWN:
				System.out.print("~");
				letItFlow();
				t.start();
			break;
			case KeyEvent.VK_PAGE_UP:
				System.out.println("Making it rain!");
				for (int x = 0; x < gridWidth; x++)
					for (int y = 0; y < gridHeight; y++)
						surfaceWater[x][y] += 1.0;
				t.start();
			break;
		}
	}
	public void mouseReleased(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}
	public static void main(String[] args) {
		JFrame frame = new JFrame("Waterrain");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new Waterrain());
		frame.pack();
		frame.setVisible(true);
	}
}
