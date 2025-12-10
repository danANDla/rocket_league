# Дискретно-событийная модель CPU

clock 100ms

# Переменные DE-уровня
var distance = 120.0
var fuel = 40.0

# ==== Реакция на SR-события ====

on FAR_FROM_TARGET -> set engine_main power 1750

on CLOSE_TO_TARGET -> set engine_main power 0

on HIGH_ANGLE -> set engine_orientation1 power 50.0

on LOW_ANGLE -> set engine_orientation1 power 0

on COLLISION_WARNING -> set engine_orientation1 power 0

# ==== Внутренние DE события, реагирующие на переменные ====

# При приближении к цели снижаем тягу
event approach:
  trigger distance < 50
  set engine_main power 0.3

# Когда очень близко — выключаем двигатель
event final_brake:
  trigger distance < 10
  set engine_main power 0.0

# Расход топлива — каждые 100 мс уменьшается
event fuel_burn:
  action update fuel -0.5