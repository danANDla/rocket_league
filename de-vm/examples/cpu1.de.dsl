# Дискретно-событийная модель CPU

clock 100ms

# Переменные DE-уровня
var distance = 120.0
var fuel = 40.0

# ==== Реакция на SR-события ====

# Событие из SR: ракета далеко по X -> чуть увеличиваем тягу
on FAR_FROM_TARGET -> set engine_main power 0.6

# Событие из SR: слишком большой угол
on HIGH_ANGLE -> set engine_orientation power 1.0

# Событие из SR: Z < 0, опасность столкновения -> аварийное торможение
on COLLISION_WARNING -> set engine_main power 0.0


# ==== Внутренние DE события, реагирующие на переменные ====

# При приближении к цели снижаем тягу
event approach:
  trigger distance < 50
  set engine_main power 0.3

# Когда очень близко — выключаем двигатель
event final_brake:
  trigger distance < 10
  set engine_main power 0.0

# Плавное уменьшение distance, симуляция движения
event drift:
  action update distance -2

# Расход топлива — каждые 100 мс уменьшается
event fuel_burn:
  action update fuel -0.5
