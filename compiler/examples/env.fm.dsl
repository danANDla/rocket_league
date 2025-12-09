tank = 50.0
consumption = 0.1
res_koef = 0.01

angle = angle + circular
acceleration = (power - resistance) / mass
mass = weight - fuel
fuel = tank - time * consumption
x = speed * cos(angle)
y = speed * sin(angle)
speed = speed + acceleration
resistance = res_koef * speed * speed
xpath = time
xdev = xpath - x
ypath = time * time
ydev = ypath - y