class VisitData {
  public String name;
  public double x;
  public double y;
  public Color col = new Color(255,255,255,255);
  
  public VisitData(String name, double x, double y){
    this.name = name;
    this.x = x;
    this.y = y;
  }
  
  public void draw(){
    stroke(50);
    fill(col.r, col.g, col.b, col.a);
    strokeWeight(0.5);
    ellipse((int)x,(int)y, 8, 8);
  }
  
}