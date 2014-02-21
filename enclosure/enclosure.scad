// =============== IMPORTS ===============
use <lib/boxes.scad>;
use <lib/cyl_head_bolt.scad>;


// =============== ENCLOSURE ===============

cornerRadius = 5; // Radius of outer box
innerCornerRadius = 5; // Radius of inner box

// PCB dimensions
pcbWidth = 100;
pcbHeight = 100;
pcbMargin = 30; // Margin between PCB and enclosure
// Thickness of outer box
outerMargin = 50;
// Calculations for total dimensions
width = pcbWidth + pcbMargin + outerMargin;
height = pcbHeight + pcbMargin + outerMargin;

totalBaseDepth = 10; // Depth of base
baseHoleDepth = 5; // Depth of holes to leave for screws
mainDepth = 30; // Depth of main enclosure
topDepth = 10; // Depth of top (lid)
totalDepth = totalBaseDepth + mainDepth + topDepth; // Total depth (for screws)

// Calculate depths for base
standDepth = totalBaseDepth - baseHoleDepth;

// Screw dimensions
screwSize = "M3";
screwHeadHeight = 3;
nutCatchDepth = 6;
screwInset = 10; // Distance from corner of screw

// Banana plug dimensions
bananaPlugRadius = 5;
bananaPlugHeight = 25; // From bottom of enclosure
bananaPlugDistances = [0,11,30,41]; // Distances of banana plugs from LHS

// Top hole dimensions (x,y,width,height)
topHoles = [[0,0,10,10], [60,60,20,20]];

// Distances of mounting slots from bottom left corner of PCB
// 3.3,0.15 in
// 0.25, 2.1 in
// 0.7, 3.65 in
// 3.65, 2.3 in
inchMultiplier = 25.4; // 1 inch in mm
mountingHoleDistances = [
	[3.3*inchMultiplier, 0.15*inchMultiplier],
	[0.25*inchMultiplier, 2.1*inchMultiplier],
	[0.7*inchMultiplier, 3.65*inchMultiplier],
	[3.65*inchMultiplier, 2.3*inchMultiplier]
];
mountingHoleRadius = 5; // Radius of hole

// To render properly
ghost = 0.1;

// Generate overall enclosure that can be laser cut
module enclosure() {
	difference() {
		union() {
			stand();
			color("red") base();
			color("green") main();
			color("pink") top();
		}

		color("blue") screwHoles();
	}
}

// Generate 3d model of final enclosure (the laser cut bit plus drilled
// holes)
module 3denclosure() {
	difference() {
		enclosure();

		// Banana plug holes for connections
		for (distance=bananaPlugDistances) {
			translate([distance,0,0]) bananaPlugHole();
		}
	}
}

module bananaPlugHole() {
	translate([-(pcbWidth+pcbMargin)/2+bananaPlugRadius*2,-height/2+outerMargin/2+ghost,bananaPlugHeight])
		rotate(a=[90,0,0])
			cylinder(h=outerMargin/2+2*ghost, r=bananaPlugRadius);
}

// Base of enclosure
module base() {
	// All above origin
	translate([0,0,standDepth+baseHoleDepth/2]) difference() {
		// Base rounded box
		roundedBox([width, height, baseHoleDepth], cornerRadius, true);

		// Minus the mounting slots
		for (distance=mountingHoleDistances) {
			translate([
				distance[0]-pcbWidth/2,
				distance[1]-pcbHeight/2,
				0
			]) mountingSlot();			
		}
	}
}

// Very bottom layer of enclosure
module stand() {
	translate([0,0,standDepth/2]) difference() {
		// Solid rounded box
		roundedBox(
			[width, height, standDepth],
			cornerRadius,
			true
		);
	}
}

// Main enclosure
module main() {
	translate([0,0,totalBaseDepth+mainDepth/2]) difference() {
		// Solid rounded box
		roundedBox([width, height, mainDepth-ghost], cornerRadius, true);

		// Minus inner rounded box to leave space for PCB
		roundedBox(
			[pcbWidth+pcbMargin, pcbHeight+pcbMargin, mainDepth+ghost],
			innerCornerRadius,
			true
		);
	}
}


// Hole for mounting PCB
module mountingSlot() {
	depth = baseHoleDepth + ghost*2;
	translate([0,0,0])
		linear_extrude(height=depth, center=true) union() {
			circle(r=mountingHoleRadius, $fn=100);
		}
}

// Top layer of enclosure (lid)
module top() {
	difference() {
		// Simple solid layer
		translate([0,0,totalBaseDepth+mainDepth+topDepth/2])
			roundedBox([width, height, topDepth-ghost], cornerRadius, true);

		// Minus top holes
		topHoles();
	}
}

// Holes in the top layer for connectors
module topHoles() {
	for (hole=topHoles) {
		translate([
			hole[0]-pcbWidth/2,
			hole[1]-pcbHeight/2,
			totalDepth-topDepth-ghost
		]) cube(size=[
			hole[2],
			hole[3],
			topDepth+2*ghost
		], center=false);
	}
}

// Screw holes to hold it all together
module screwHoles() {
	translate([width/2-screwInset,height/2-screwInset,0]) screwHole();
	translate([width/2-screwInset,-height/2+screwInset,0]) screwHole();
	translate([-width/2+screwInset,height/2-screwInset,0]) screwHole();
	translate([-width/2+screwInset,-height/2+screwInset,0]) screwHole();
}
module screwHole() {
	// Main screw hole for screw to clear through
	translate([0,0,totalDepth+ghost]) hole_through(
		name=screwSize,
		l=totalDepth+2*ghost,
		h=0 // TODO Change this for final layer
	);

	translate([0,0,nutCatchDepth+ghost]) nutcatch_parallel(
		screwSize,
		l=nutCatchDepth+2*ghost
	);
}


// =============== SLICING ===============
// Material thickness
allLayerHeight = 5;

// Layer height for the stand
standLayerHeight = allLayerHeight;
standLayers = ceil(standDepth/standLayerHeight);

// Layer height for the base (thin bit with key slots in)
// (keySlotDepth = 1.57 should be ~an integer multiple of this)
baseLayerHeight = allLayerHeight;
baseLayers = ceil(baseHoleDepth/baseLayerHeight);

// Layer height for the main enclosure
mainLayerHeight = allLayerHeight;
mainLayers = ceil(mainDepth/mainLayerHeight);

// Layer height for the top enclosure
topLayerHeight = allLayerHeight;
topLayers = ceil(topDepth/topLayerHeight);

// Boundary to leave between adjacent slices
sliceBoundary = 10;

// Total distance from one slice to the next in each dim
sliceWidth = width + sliceBoundary;
sliceHeight = height + sliceBoundary;

module slice() {
	// Slice all the stand layers
	for (i=[0:standLayers-1]) {
		translate([i*sliceWidth, 0]) layer(i*standLayerHeight);
	}

	// Slice all the base layers below the stand layers
	for (i=[0:baseLayers-1]) {
		translate([i*sliceWidth, sliceHeight])
			layer(standDepth + i*baseLayerHeight);
	}

	// Slice all the main layers below that
	for (i=[0:mainLayers-1]) {
		translate([i*sliceWidth, 2*sliceHeight])
			layer(totalBaseDepth + i*mainLayerHeight);
	}

	// Slice all the top layers below that
	for (i=[0:topLayers-1]) {
		translate([i*sliceWidth, 3*sliceHeight])
			layer(totalBaseDepth+mainDepth + i*mainLayerHeight);
	}
}



// Get a layer slice at a certain z height
module layer(z) {
	projection(cut=true)
		translate([0,0,-(z+ghost)]) enclosure();
}


// =============== INTERFACE ===============
// slice() for slices or 3denclosure() for 3D model
slice();