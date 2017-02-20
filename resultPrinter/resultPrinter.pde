import org.gicentre.utils.move.*;
import org.gicentre.utils.stat.*;
import java.awt.event.KeyEvent;
import drop.*;


double timeSum = 0.0;
ArrayList<Double> times = new ArrayList<Double>();

double emSum = 0.0;
ArrayList<Double> emissions = new ArrayList<Double>();

double distSum = 0.0;
ArrayList<Double> distances = new ArrayList<Double>();

double costSum = 0.0;
ArrayList<Double> costs = new ArrayList<Double>();

VisitData depot;
ArrayList<ArrayList<VisitData>> routes = new ArrayList<ArrayList<VisitData>>();

BarChart barChart = null;

class Color {
  int r, g, b, a;
  Color() {
    this(50 + (int)random(200), 50 + (int)random(200), 50 + (int)random(200), 255);
  }
  Color(int r, int g, int b, int a) {
    this.r = r; 
    this.g = g; 
    this.b = b; 
    this.a = a;
  }
}
ArrayList<Color> colours = new ArrayList<Color>();

enum DrawModes {
  ROUTES, 
    CHART
}
DrawModes drawMode = DrawModes.ROUTES;

enum ChartVars {
  TIME, 
    EMISSIONS, 
    DISTANCE, 
    COST
}
ChartVars chartVar = ChartVars.TIME;

SDrop drop;

ZoomPan zoomer;

String fileName = "";

void setup() {
  frameRate(30);
  size(640, 640);

  drop = new SDrop(this);

  zoomer = new ZoomPan(this);
}

void fileSelected(File selection) {
  noLoop();
  print((int)(1000 * 1.0 / (frameRate*2)));
  delay((int)(frameRate*2));

  if (selection == null) {
    println("No files selected, closing..");
    exit();
  } else {
    println("User selected " + selection.getAbsolutePath());

    fileName = selection.getName();

    // Reset Time Information
    timeSum = 0.0;
    times = new ArrayList<Double>();

    // Reset Emissions Information
    emSum = 0.0;
    emissions = new ArrayList<Double>();

    // Reset Distance Information
    distSum = 0.0;
    distances = new ArrayList<Double>();

    // Reset Cost Information
    costSum = 0.0;
    costs = new ArrayList<Double>();

    // Reset the routes
    routes = new ArrayList<ArrayList<VisitData>>();

    //Reset the route colours
    colours = new ArrayList<Color>();

    // Get Vals
    String[] lines = loadStrings(selection);
    for (String line : lines) {
      String[] elements = line.split(",");
      if (elements.length > 0) {
        switch(elements[0]) {
        case "Time":
          times.add(Double.valueOf(elements[1]));
          break;
        case "Emissions":
          emissions.add(Double.valueOf(elements[1]));
          break;
        case "Distance":
          distances.add(Double.valueOf(elements[1]));
          break;
        case "Cost":
          costs.add(Double.valueOf(elements[1]));
          break;
        }
      }
    }
    // Get Sums
    timeSum = sumList(times);
    emSum = sumList(emissions);
    distSum = sumList(distances);
    costSum = sumList(costs);

    loadRoutes(lines);
  }

  loop();
}

void loadRoutes(String[] lines) {
  println("Load Routes");
  int routeNum = 0;
  for (int i = 0; i < lines.length; i++) {
    if (lines[i].startsWith("Depot Name")) {
      String[] elements = lines[i+1].split(",");
      depot = new VisitData(elements[0], Double.valueOf(elements[2]), Double.valueOf(elements[3]));
    }

    if (lines[i].startsWith("Route Name")) {
      ArrayList<VisitData> newRoute = new ArrayList<VisitData>();
      randomSeed(routeNum);
      Color routeCol = new Color();
      colours.add(routeCol);
      for (int j = i-1; j > 0; j--) {
        if (lines[j].startsWith("Visit Name")) {
          break;
        }
        String[] elements = lines[j].split(",");
        VisitData newVisit = new VisitData(elements[0], Double.valueOf(elements[2]), Double.valueOf(elements[3]));
        newVisit.col = routeCol;
        newRoute.add(0, newVisit);
      }
      routes.add(newRoute);
      routeNum++;
    }
  }

  // Correcting Offsets
  float maxMagnitude = 0.0;
  PVector center = new PVector(0.0, 0.0);
  for (ArrayList<VisitData> route : routes) {
    for (VisitData visit : route) {
      visit.x -= depot.x;
      visit.y -= depot.y;

      PVector pos = new PVector((float) visit.x, (float) visit.y);
      center.add(pos);

      maxMagnitude = max(maxMagnitude, pos.mag());
    }
  }

  depot.x = 0;
  depot.y = 0;

  for (ArrayList<VisitData> route : routes) {
    for (VisitData visit : route) {
      visit.x *= max(width/2, height/2) / maxMagnitude;
      visit.y *= max(width/2, height/2) / maxMagnitude;
    }
  }

  //center.div(max(width/2, height/2) * maxMagnitude);
  // zoomer.setPanOffset(center.x * width/2, center.y * height/2);
  zoomer = new ZoomPan(this);
  zoomer.allowZoomButton(false);
  zoomer.setPanOffset(width/2, height/2);

  setupChart();

  println(routeNum +" Routes");
}

void drawInfo() {
  stroke(153);
  textSize(14);
  textAlign(CENTER, TOP);
  text("File: "+ fileName, width/2, 0);
  text("Time:"+ timeSum, width/2, 14);
  text("Emissions:"+ emSum, width/2, 28);
  text("Distance:"+ distSum, width/2, 42);
  text("Cost:"+ costSum, width/2, 56);
}



void drawRoutes() {

  zoomer.transform();
  VisitData prev = depot;

  // Draw Connections
  int colIndex = 0;
  for (ArrayList<VisitData> route : routes) {
    prev = depot;
    for (VisitData visit : route) {
      Color c = colours.get(colIndex);
      stroke(c.r, c.g, c.b, c.a);
      strokeWeight(1);
      line((int)prev.x, (int)prev.y, (int)visit.x, (int) visit.y);
      prev = visit;
    }

    line((int)prev.x, (int)prev.y, (int)depot.x, (int) depot.y);

    colIndex++;
  }

  for (ArrayList<VisitData> route : routes) {
    prev = depot;
    for (VisitData visit : route) {
      visit.draw();
      prev = visit;
    }
  }

  depot.draw();
}


void drawChart() {
  if (barChart != null) {
    barChart.draw(15, 60, width-30, height-70);
    fill(60);
    stroke(60);
    textSize(20);
    switch(chartVar) {
    case TIME:
      text("Time per route", width/2 + 30, 30);
      break;
    case EMISSIONS:
      text("Emissions per route", width/2 + 30, 30);
      break;
    case DISTANCE:
      text("Distance per route", width/2 + 30, 30);
      break;
    case COST:
      text("Cost per route", width/2 + 30, 30);
      break;
    }
    textSize(11);
    text("", 
      width/2, 50);
  } else {
    setupChart();
  }
}


void draw() {
  if (fileName == "") {
    background(0);
    textAlign(CENTER);
    text("Click or Drag a Results File", width/2, height/2);
  } else {
    switch(drawMode) {
    case ROUTES:
      background(120);
      drawInfo();
      drawRoutes();
      break;
    case CHART:
      background(255);
      drawChart();
      break;
    }
  }
}

void setupChart() {  
  if (fileName != "") {    
    barChart = new BarChart(this);
    float[] data = new float[routes.size()];
    String[] labels = new String[routes.size()];
    float maxVal = 0.0;
    for (int i = 0; i < routes.size(); i++) {
      switch(chartVar) {
      case TIME:
        barChart.setValueAxisLabel("Time");
        data[i] = (float) (double)times.get(i);
        break;
      case EMISSIONS:
        barChart.setValueAxisLabel("Emsisions");
        data[i] = (float) (double)emissions.get(i);
        break;
      case DISTANCE:
        barChart.setValueAxisLabel("Distance");
        data[i] = (float) (double)distances.get(i);
        break;
      case COST:
        barChart.setValueAxisLabel("Cost");
        data[i] = (float) (double)costs.get(i);
        break;
      }
      labels[i] = String.valueOf(i);
      maxVal = max(maxVal, data[i]);
    }
    barChart.setData(data);
    barChart.setBarLabels(labels);

    barChart.showValueAxis(true);
    barChart.setValueFormat("#");

    barChart.setCategoryAxisLabel("Route");
    barChart.showCategoryAxis(true);

    // Scaling
    barChart.setMinValue(0);
    barChart.setMaxValue(maxVal);

    barChart.setBarColour(color(200, 80, 80, 150));
    barChart.setBarGap(4);
  }
}

void keyPressed() {
  if (key == CODED) {
    if (keyCode == KeyEvent.VK_F1) {
      drawMode = DrawModes.ROUTES;
      barChart = null;
    } else if (keyCode == KeyEvent.VK_F2) {
      drawMode = DrawModes.CHART;
      chartVar = ChartVars.TIME;
      setupChart();
    } else if (keyCode == KeyEvent.VK_F3) {
      drawMode = DrawModes.CHART;
      chartVar = ChartVars.EMISSIONS;
      setupChart();
    } else if (keyCode == KeyEvent.VK_F4) {
      drawMode = DrawModes.CHART;
      chartVar = ChartVars.DISTANCE;
      setupChart();
    } else if (keyCode == KeyEvent.VK_F5) {
      drawMode = DrawModes.CHART;
      chartVar = ChartVars.COST;
      setupChart();
    }
  }
}

void mouseClicked() {
  if (fileName == "") {
    selectInput("Select a result file to process:", "fileSelected");
  }
}

double sumList(ArrayList<Double> toSum) {
  double sum = 0.0;
  for (double item : toSum)
    sum += item;
  return sum;
}

void dropEvent(DropEvent event) {
  if (event.isFile()) {
    fileSelected(event.file());
  }
}