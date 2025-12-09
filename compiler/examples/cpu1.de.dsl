clock 100
var xproection = 0.0
var yproection = 0.0
var xrelative = 0.0
var yrelative = 0.0
var rotate_correction = 0.0

on XDEV_ERROR -> add angle rotate_correction 
on YDEV_ERROR -> add angle rotate_correction

event x_proection:
 action update xproection sin(rotate)

event y_proection:
 action update yproection cos(rotate)

event x_relative:
 action update xrelative xdev / xproection

event y_relative:
 action update yrelative ydev / yproection

event r_correction:
 action update rotate_correction yrelative - xrelative

event power_changer:
 trigger abs(rotate_correction) < 0.1
 add power 1
