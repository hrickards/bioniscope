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

baseDepth = 5; // Depth of base
keySlotDepth = 1.57; // Depth of layer for key slots
mainDepth = 20; // Depth of main enclosure
topDepth = 5; // Depth of top (lid)
totalDepth = baseDepth + mainDepth + topDepth; // Total depth (for screws)

// Screw dimensions
screwSize = "M3";
screwHeadHeight = 3;
nutCatchDepth = 6;
screwInset = 10; // Distance from corner of screw

// Banana plug dimensions
bananaPlugRadius = 5;
bananaPlugHeight = 15; // From bottom of enclosure
bananaPlugDistances = [0,11,30,41]; // Distances of banana plugs from LHS

// Top hole dimensions (x,y,width,height)
topHoles = [[0,0,10,10], [60,60,20,20]];

// Key slot dimensions (from datasheet)
keySlotCutoutRadius = 15;

// Distances of mounting slots from bottom left corner of PCB
// 3.3,0.15 in
// 0.25, 2.1 in
// 0.7, 3.65 in
// 3.65, 2.3 in
inchMultiplier = 25.4; // 1 inch in mm
keySlotDistances = [
	[3.3*inchMultiplier, 0.15*inchMultiplier],
	[0.25*inchMultiplier, 2.1*inchMultiplier],
	[0.7*inchMultiplier, 3.65*inchMultiplier],
	[3.65*inchMultiplier, 2.3*inchMultiplier]
];

// To render properly
ghost = 0.1;

// Generate overall enclosure that can be laser cut
module enclosure() {
	difference() {
		union() {
			color("red") base();
			color("green") main();
			stand();
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
	translate([0,0,baseDepth-keySlotDepth/2]) difference() {
		// Base rounded box
		roundedBox([width, height, keySlotDepth], cornerRadius, true);

		// Minus the mounting slots
		for (distance=keySlotDistances) {
			translate([
				distance[0]-pcbWidth/2,
				distance[1]-pcbHeight/2,
				baseDepth/2-keySlotDepth
			]) mountingSlot();			
		}
	}
}

// Very bottom layer of enclosure
// Keeps the key slots accessible
module stand() {
	translate([0,0,(baseDepth-keySlotDepth)/2]) difference() {
		// Solid rounded box
		roundedBox(
			[width, height, baseDepth-keySlotDepth],
			cornerRadius,
			true
		);

		// Circle cutouts around keyslots
		for (distance=keySlotDistances) {
			translate([
				distance[0]-pcbWidth/2,
				distance[1]-pcbHeight/2,
				(keySlotDepth-baseDepth)/2
			]) keySlotCutout();		
		}
	}
}

// Circular cutout for the key slot
module keySlotCutout() {
	translate([0,0,-ghost])
		cylinder(r=keySlotCutoutRadius, h=baseDepth-keySlotDepth+2*ghost);
}

// Main enclosure
module main() {
	translate([0,0,baseDepth+mainDepth/2]) difference() {
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


// Key slot for mounting PCB
module mountingSlot() {
	// From datasheet
	joinLength = 4.3;
	smallRadius = 1.6;
	bigRadius = 3.05;
	depth = keySlotDepth+ghost*2;

	// Centered around small circle

	translate([0,0,-depth/2])
		linear_extrude(height=depth, center=true) union() {
			circle(r=smallRadius, $fn=100);
			translate([joinLength,0,0])
				circle(r=bigRadius, $fn=100);
	
			translate([joinLength/2,0])
				square([joinLength,smallRadius*2], center=true);
		}
}

// Top layer of enclosure (lid)
module top() {
	difference() {
		// Simple solid layer
		translate([0,0,baseDepth+mainDepth+topDepth/2])
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

// Layer height for the stand (of height baseDepth-keySlotDepth)
standLayerHeight = 3;
standLayers = ceil((baseDepth-keySlotDepth)/standLayerHeight);

// Layer height for the base (thin bit with key slots in)
// (keySlotDepth = 1.57 should be ~an integer multiple of this)
baseLayerHeight = 1.57;
baseLayers = ceil(keySlotDepth/baseLayerHeight);

// Layer height for the main enclosure
mainLayerHeight = 5;
mainLayers = ceil(mainDepth/mainLayerHeight);

// Layer height for the top enclosure
topLayerHeight = 5;
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
			layer((baseDepth-keySlotDepth) + i*baseLayerHeight);
	}

	// Slice all the main layers below that
	for (i=[0:mainLayers-1]) {
		translate([i*sliceWidth, 2*sliceHeight])
			layer(baseDepth + i*mainLayerHeight);
	}

	// Slice all the top layers below that
	for (i=[0:topLayers-1]) {
		translate([i*sliceWidth, 3*sliceHeight])
			layer(baseDepth+mainDepth + i*mainLayerHeight);
	}
}




// Standard layer height
layerHeight = 5;

// Heights to slice at
sliceHeights = [
	0, // Bottom layer
	baseDepth - keySlotDepth,
	5
];

// Get a layer slice at a certain z height
module layer(z) {
	projection(cut=true)
		translate([0,0,-(z+ghost)]) enclosure();
}

//module slice() {
//	for (i = [0:len(sliceHeights)-1]) {
//		translate([i*(width+sliceBoundary),0]) layer(sliceHeights[i]);
//	}
//}



// =============== INTERFACE ===============
// slice() for slices or 3denclosure() for 3D model
3denclosure();