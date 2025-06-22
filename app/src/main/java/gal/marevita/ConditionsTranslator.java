package gal.marevita;

import java.util.List;

public class ConditionsTranslator {

    public static final List<String> allDefaultConditions = List.of("temperature", "humidity", "wind_speed", "wind_direction", "pressure", "precipitation", "cloud_cover", "illumination", "moon_age", "wave_height", "wave_direction", "wave_period", "sea_level", "sea_temperature", "current_velocity", "current_direction");

    public static String translateGalego(String condition) {
        switch (condition) {
            case "temperature":
                return "Temperatura";
            case "humidity":
                return "Humidade relativa";
            case "wind_speed":
                return "Velocidade do vento";
            case "wind_direction":
                return "Dirección do vento";
            case "pressure":
                return "Presión atmosférica";
            case "precipitation":
                return "Precipitacións";
            case "cloud_cover":
                return "Cobertura das nubes";
            case "wave_height":
                return "Altura de onda";
            case "wave_direction":
                return "Dirección das ondas";
            case "wave_period":
                return "Período das ondas";
            case "sea_level":
                return "Nivel do mar";
            case "sea_temperature":
                return "Temperatura do mar";
            case "current_velocity":
                return "Velocidade das correntes";
            case "current_direction":
                return "Dirección das correntes";
            case "illumination":
                return "Iluminación da lúa";
            case "moon_age":
                return "Día do ciclo lunar";
            default:
                return condition;
        }
    }

    public static String translateEnglish(String condition) {
        switch (condition) {
            case "Temperatura":
                return "temperature";
            case "Humidade relativa":
                return "humidity";
            case "Velocidade do vento":
                return "wind_speed";
            case "Dirección do vento":
                return "wind_direction";
            case "Presión atmosférica":
                return "pressure";
            case "Precipitacións":
                return "precipitation";
            case "Cobertura das nubes":
                return "cloud_cover";
            case "Altura de onda":
                return "wave_height";
            case "Dirección das ondas":
                return "wave_direction";
            case "Período das ondas":
                return "wave_period";
            case "Nivel do mar":
                return "sea_level";
            case "Temperatura do mar":
                return "sea_temperature";
            case "Velocidade das correntes":
                return "current_velocity";
            case "Dirección das correntes":
                return "current_direction";
            case "Iluminación da lúa":
                return "illumination";
            case "Día do ciclo lunar":
                return "moon_age";
            default:
                return condition;
        }
    }

    public static String getMesurementUnit(String condition) {
        switch (condition) {
            case "temperature":
                return "ºC";
            case "humidity":
                return "%";
            case "wind_speed":
                return "km/h";
            case "wind_direction":
                return "º";
            case "pressure":
                return "hPa";
            case "precipitation":
                return "mm";
            case "cloud_cover":
                return "%";
            case "wave_height":
                return "m";
            case "wave_direction":
                return "º";
            case "wave_period":
                return "s";
            case "sea_level":
                return "m";
            case "sea_temperature":
                return "ºC";
            case "current_velocity":
                return "km/h";
            case "current_direction":
                return "º";
            case "illumination":
                return "%";
            case "moon_age":
                return "días";
            default:
                return "";
        }
    }

}
