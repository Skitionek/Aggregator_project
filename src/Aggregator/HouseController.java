package Aggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dtu.is31380.AbstractHouseController;
import dtu.is31380.ActuatorConfig;
import dtu.is31380.HouseControllerInterface;
import dtu.is31380.SensorConfig;

public class HouseController extends AbstractHouseController {

    protected static long initTime = 0;

    private static long clockZero;
    private static double tStart_next;
    private static double tEnd_next;
    private static double tEnd_curr;
    private static double t_req;
    private static double T_set;
    private static double deviation;
    private double T_bandmax;
    private double T_bandmin;
    private static double Tminreg;
    private static double Tmaxreg;
    private static boolean regulating;
    private static boolean going_back;
    private static String current_req;
    private static String next_req;
    private static double average;
    private static long t_act;
    private static double flexibility = 1;
    public static final long RESOLUTION = 1000; //run everything on a 1s basis

    protected static final int TIME_STEP = 1000;
    public HouseController() {
        super(TIME_STEP); //set timestep to 5000ms
        NAME = "house" + Math.random();
        //clockZero=System.currentTimeMillis()-initTime;
        T_set = 21;
        deviation = 0.25;
        T_bandmax = T_set + deviation;
        T_bandmin = T_set - deviation;
        Tminreg = 19;
        Tmaxreg = 24;
        tStart_next = 0;
        tEnd_next = 0;
        tEnd_curr = 0;
        regulating = false;
        current_req = null;
        next_req = "";
        t_act = 0;
        t_req = 0;
        going_back = false;
    }

    //ACTIVATION SIGNALS (TURN ON, OFF OF NORMAL)
    public enum State {
        MAX,
        NORM,
        MIN
    };
    protected static State state = State.NORM;

    //ACTIVATION SIGNALS (TURN ON, OFF OF NORMAL)
    public enum RegulationType {
        UP,
        DOWN
    };
    private RegulationType regulation = RegulationType.UP;

    protected static String NAME = null;

    private static long simulation = clockZero;



    @
    Override
    protected void execute() {
        simulation = (long) Math.ceil((System.currentTimeMillis() - initTime) / 1000);
        //current request should b set to null after regulation ends
        if (simulation > tEnd_curr - 5 && tEnd_next != 0) { //find a better way to end control after contract period
            System.out.println("not good" + ", time_end" + tEnd_curr);
            current_req = next_req;
            state = state.NORM;
            going_back = true;
            tEnd_curr = tEnd_next;
            System.out.println("not good" + ", time_end" + tEnd_curr);
        }
        HouseControllerInterface intf = getInterface();
        List < String > heaters = get_heaters();
        List < String > sensors = get_sensors();
        double sum = 0;
        average = 0;

        for (String sens: sensors) {
            sum += intf.getSensorValue(sens);
            average = sum / sens.length();
        }

        if (going_back && average > T_bandmin && average < T_bandmax) {
            regulating = false;
            going_back = false;

            System.out.println("I'm back in normal operation!");
        }

        if (state == State.NORM) {
            //System.out.println("Time: "+simulation+","+" House in NORM state");
            if (average < T_bandmin) {

                for (String heat: heaters) {
                    intf.setActuator(heat.toString(), 1.0);
                }
            } else if (average > T_bandmax) {
                for (String heat: heaters) {
                    intf.setActuator(heat.toString(), 0.0);
                }
            }
        } else if (state == State.MAX) {
            regulating = true;
            //t_act=simulation;
            //System.out.println("Time: "+simulation+","+" House in MAX state");
            for (String heat: heaters) {
                intf.setActuator(heat.toString(), 1.0);
            }

            if (average > Tmaxreg) {
                for (String heat: heaters) {
                    intf.setActuator(heat.toString(), 0.0);
                    state = State.NORM; //CHANGE TO NORM
                    going_back = true;
                }
            }
        } else if (state == State.MIN) {
            //t_act=simulation;
            regulating = true;
            //System.out.println("Time: "+simulation+","+" House in MIN state");
            for (String heat: heaters) {
                intf.setActuator(heat.toString(), 0.0);
            }

            if (average < Tminreg) {
                for (String heat: heaters) {
                    intf.setActuator(heat.toString(), 1.0);
                    state = State.NORM; //CHANGE TO NORM
                    going_back = true;

                }
            }
        }
    }

    private List < String > get_heaters() {
        HouseControllerInterface intf = getInterface();
        List < String > heaters = new ArrayList < String > ();
        for (ActuatorConfig actuator: intf.getBuildingConfig().getAllActuators()) {
            Matcher match =
                Pattern.compile("htr").matcher(actuator.getName().toString());
            if (match.find()) {
                heaters.add(actuator.getName().toString());
            }
        }
        return heaters;
    }

    private List < String > get_sensors() {
        HouseControllerInterface intf = getInterface();
        List < String > sensors = new ArrayList < String > ();
        for (SensorConfig sensor: intf.getBuildingConfig().getAllSensors()) {
            Matcher match =
                Pattern.compile("(tempr)|(tempm)").matcher(sensor.getName().toString());
            if (match.find()) {
                sensors.add(sensor.getName().toString());
            }
        }
        return sensors;
    }
    protected static double flexibility_at_t0(RegulationType type, double t_start, double t_end) {
        if (type == RegulationType.UP) {
            next_req = "up";
            tStart_next = t_start + 180;
            tEnd_next = t_end + 180;
            t_act = (long)(tStart_next + 1);
            System.out.println("Time start: " + tStart_next + "," + " Time_end " + tEnd_next + ", reg_type=UP " + type);
            t_req = simulation;
            return get_time("up");
        } else {
            next_req = "down";
            tStart_next = t_start + 180;
            tEnd_next = t_end + 180;
            t_act = (long)(tStart_next + 1);
            System.out.println("Time start: " + tStart_next + "," + " Time_end " + tEnd_next + ", reg_type=down " + type);
            t_req = simulation;
            return get_time("down");
        }
    }
    private static double get_time(String req) {

        HouseControllerInterface intf = getInterface();
        double T_out = intf.getSensorValue("s_tempout");
        double Cdown = 0.0028 * T_out;
        double Cup = 0.000382 * T_out;
        double T_down = T_set + deviation;
        double T_up = T_set - deviation;
        double timeup = (T_up - Tminreg) / Cup;
        double timedown = (Tmaxreg - T_down) / Cdown;
        System.out.println("UP_reg= " + timeup);
        System.out.println("Down_reg= " + timedown);
        System.out.println("regulation= " + regulating);
        System.out.println("cur_req= " + current_req);
        if (!regulating && current_req == null) {
            System.out.println("within IF");
            if (req == "up") {
                System.out.println("should be this");
                return timeup;
            } else if (req == "down") {
                return timedown;
            } else {
                return 0;
            }
        } else if (!regulating && current_req != null) {
            System.out.println("within IF not initial");
            double period = tStart_next - t_req;
            System.out.println("interval=" + period);
            switch (current_req) {
                case "up":
                    if (timeup > period) {
                        double T = (T_up - period * Cup);
                        System.out.println("temp=" + T);
                        if (req == "up") {
                            System.out.println("not regulating1 " + "interval=" + period + ", request=" + req + ", time=" + (T - Tminreg) / Cup);
                            return (T - Tminreg) / Cup;
                        } else if (req == "down") {
                            System.out.println("not regulating1 " + ", interval=" + period + ", request=" + req + ", time=" + (Tmaxreg - T_down) / Cdown);
                            return (Tmaxreg - T_down) / Cdown;
                        } else {
                            return 0;
                        }
                    } else if (timeup < period) {
                        double t = t_req + timeup;
                        System.out.println("t=" + t);
                        if (req == "up") {
                            System.out.println("not regulating2 " + "interval=" + t + ", request=" + req + ", time=" + (tStart_next - t) * Cdown / Cup);
                            return (tStart_next - t) * Cdown / Cup;
                        } else if (req == "down") {
                            return timedown;
                        } else {
                            return 0;
                        }
                    } else {
                        return 0;
                    }
                case "down":
                    if (timedown > period) {
                        double T = (T_down + period * Cdown);
                        System.out.println("temp=" + T);
                        if (req == "up") {
                            return timeup;
                        } else if (req == "down") {
                            System.out.println("not regulating2 " + "temp=" + T + ", request=" + req + ", time=" + (Tmaxreg - T) / Cdown);
                            return (Tmaxreg - T) / Cdown;
                        } else {
                            return 0;
                        }
                    } else if (timedown < period) {
                        double t = t_req + timedown;
                        System.out.println("t=" + t);
                        if (req == "up") {
                            return timeup;
                        } else if (req == "down") {
                            System.out.println("not regulating2 " + "tstart_next=" + tStart_next + "t=" + t + ", request=" + req + ", time=" + (tStart_next - t) * Cup / Cdown);
                            return (tStart_next - t) * Cup / Cdown;
                        } else {
                            return 0;
                        }
                    } else {
                        return 0;
                    }
                default:
                    return 0;
            }
        } else if (regulating && current_req != null) {
            System.out.println("within IF regulating");
            double period = tStart_next - t_act;
            switch (current_req) {
                case "up":
                    if (timeup > period) {
                        double T = (T_up - period * Cup);
                        System.out.println("type up");
                        if (req == "up") {
                            System.out.println("regulating1 " + "tstart_next=" + tStart_next + "temp=" + T + ", request=" + req + ", time=" + (T - Tminreg) / Cup);
                            return (T - Tminreg) / Cup;
                        } else if (req == "down") {
                            return timedown;
                        } else {
                            return 0;
                        }
                    } else if (timeup < period) {
                        double t = t_act + timeup;
                        if (req == "up") {
                            System.out.println("regulating1 " + "tstart_next=" + tStart_next + "t=" + t + ", request=" + req + ", time=" + (tStart_next - t) * Cdown / Cup);
                            return (tStart_next - t) * Cdown / Cup;
                        } else if (req == "down") {
                            return timedown;
                        } else {
                            return 0;
                        }
                    } else {
                        return 0;
                    }
                case "down":
                    if (timedown > period) {
                        double T = (T_down + period * Cdown);
                        if (req == "up") {
                            return timeup;
                        } else if (req == "down") {
                            System.out.println("regulating2 " + "tstart_next=" + tStart_next + "temp=" + T + ", request=" + req + ", time=" + (Tmaxreg - T) / Cdown);
                            return (Tmaxreg - T) / Cdown;
                        } else {
                            return 0;
                        }
                    } else if (timedown < period) {
                        double t = t_act + timedown;
                        if (req == "up") {
                            return timeup;
                        } else if (req == "down") {
                            System.out.println("regulating2" + "tstart_next=" + tStart_next + "t=" + t + ", request=" + req + ", time=" + (tStart_next - t) * Cdown / Cup);
                            return (tStart_next - t) * Cup / Cdown;
                        } else {
                            return 0;
                        }
                    } else {
                        return 0;
                    }
                default:
                    return 0;
            }
        } else {
            return 0;
        }
    }

    @
    Override
    protected void init() {
        HouseControllerInterface intf = getInterface();
        List < String > heaters = get_heaters();
        for (String heat: heaters) {
            intf.setActuator(heat.toString(), 0.0);
        }

    };


}