# Синхронно-реактивные правила на основе continuous входов
# Эти события будут использоваться в дискретно-событийном узле

# Если дистанция по оси X становится слишком большой — надо ускоряться
rule coordinates_x < 1850 -> FAR_FROM_TARGET

rule coordinates_x > 1850 -> CLOSE_TO_TARGET

# Если ракета слишком сильно наклонена — корректировать ориентацию
rule coordinates_x > 2800 -> HIGH_ANGLE

rule coordinates_x < 2800 -> LOW_ANGLE