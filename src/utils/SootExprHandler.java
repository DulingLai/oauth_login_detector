package utils;

import config.GlobalConfigs;
import soot.*;
import soot.jimple.InvokeExpr;

import java.util.List;
import java.util.Optional;

public class SootExprHandler {
    public static String[] handleOAuthInvokeExpr(Body b, Unit u, SootClass c, InvokeExpr invokeExpr, FastHierarchy classHierarchy) {
        String className = c.getName();
        String methodName = invokeExpr.getMethod().getName();
        String methodClass = invokeExpr.getMethod().getDeclaringClass().getName();
        String[] results = {"None", "None"};

        // Google
        if (OAuthProviders.detectGoogle(className, methodName, methodClass)) {
            results[0] = "Google";
//             Check if the current class is an activity
            if (classHierarchy.isSubclass(c, Scene.v().getSootClass("android.app.Activity"))) {
                results[1] = className;
            }

            // print to a file for debug
            String printResults = results[0] + "\t" + results[1];
            utils.FileUtils.printFile(GlobalConfigs.RESULT_FILE, printResults);

            // TODO: what if the method is not in an activity but in a class called by an activity
//            else {
//                if(classHierarchy.isClassSubclassOf(c, Scene.v().getSootClass("android.app.Activity"))){
//
//                }
//            }
        }

        // GoogleMigrate
        if (OAuthProviders.detectGoogleMigrate(b, u, className, invokeExpr)) {
            results[0] = "GoogleMitigate";
            // Check if the current class is an activity
            if (classHierarchy.isSubclass(c, Scene.v().getSootClass("android.app.Activity"))) {
                results[1] = className;
            }

            // print to a file for debug
            String printResults = results[0] + "\t" + results[1];
            utils.FileUtils.printFile(GlobalConfigs.RESULT_FILE, printResults);
        }

        // GoogleV1
        if (OAuthProviders.detectGoogleV1(b, u, className, invokeExpr)) {
            results[0] = "GoogleV1";
            // Check if the current class is an activity
            if (classHierarchy.isSubclass(c, Scene.v().getSootClass("android.app.Activity"))) {
                results[1] = className;
            }

            // print to a file for debug
            String printResults = results[0] + "\t" + results[1];
            utils.FileUtils.printFile(GlobalConfigs.RESULT_FILE, printResults);
        }

//        // Facebook
//        if (OAuthProviders.detectFacebook(className, methodName, methodClassName)) {
//            if (!info.contains("Facebook")) {
//                info.add("Facebook");
//            }
//            detected = true;
//        }
//
//        // Facebook V3
//        if (OAuthProviders.detectFacebookV3(className, methodName, methodClassName)) {
//            if (!info.contains("FacebookV3")) {
//                info.add("FacebookV3");
//            }
//            detected = true;
//        }
//
//        // Twitter Authentication
//        if (OAuthProviders.detectTwitterAuthentication(className, methodName, methodClassName)) {
//            if (!info.contains("TwitterAuthentication")) {
//                info.add("TwitterAuthentication");
//            }
//            detected = true;
//        }
//
//        // Twitter Authorization
//        if (OAuthProviders.detectTwitterAuthorization(className, methodName, methodClassName)) {
//            if (!info.contains("TwitterAuthorization")) {
//                info.add("TwitterAuthorization");
//            }
//            detected = true;
//        }
//
//        // Twitter4J
//        if (OAuthProviders.detectTwitter4J(className, methodName, methodClassName)) {
//            if (!info.contains("Twitter4J")) {
//                info.add("Twitter4J");
//            }
//            detected = true;
//        }
//
//        // VK
//        if (OAuthProviders.detectVK(className, methodName, methodClassName)) {
//            if (!info.contains("VK")) {
//                info.add("VK");
//            }
//            detected = true;
//        }
//
//        // LinkedIn
//        if (OAuthProviders.detectLinkedIn(className, methodName, methodClassName)) {
//            if (!info.contains("LinkedIn")) {
//                info.add("LinkedIn");
//            }
//            detected = true;
//        }
//
//        // Amazon
//        if (OAuthProviders.detectAmazon(className, methodName, methodClassName)) {
//            if (!info.contains("Amazon")) {
//                info.add("Amazon");
//            }
//            detected = true;
//        }
//
//        // Foursquare
//        if (OAuthProviders.detectFoursquare(className, methodName, methodClassName)) {
//            if (!info.contains("Foursquare")) {
//                info.add("Foursquare");
//            }
//            detected = true;
//        }
        return results;
    }

    public static Optional<String[]> findResourceId(Body b, Unit u, SootClass c, InvokeExpr invokeExpr) {
        String className = c.getName();
        String methodName = invokeExpr.getMethod().getName();
        String methodClass = invokeExpr.getMethod().getDeclaringClass().getName();
        List<Value> argValue = invokeExpr.getArgs();

        // Detect all the widgets resource-ids
        if (methodName.equals("findViewById")) {
            if(argValue != null) {
                String arg = argValue.get(0).toString();
//                    if (config.Constants.debug) {
//                        System.out.println(className + ": " + methodClass + "." + methodName + "(" + arg + ")");
//                    }
                String[] results = {className, methodClass, methodName, arg};
                return Optional.of(results);
            }
        }

        return Optional.empty();
    }

    public static Optional<String[]> findTextLabels(Body b, Unit u, SootClass c, InvokeExpr invokeExpr) {
        String className = c.getName();
        String methodName = invokeExpr.getMethod().getName();
        String methodClass = invokeExpr.getMethod().getDeclaringClass().getName();
        List<Value> argValue = invokeExpr.getArgs();

        // Detect all the widgets resource-ids
        if (methodName.equals("setContentDescription")) {
            if(argValue != null) {
                String arg = argValue.toString();
//                    if (config.Constants.debug) {
//                        System.out.println(className + ": " + methodClass + "." + methodName + "(" + arg + ")");
//                    }
                String[] results = {className, methodClass, methodName, arg};
                return Optional.of(results);
            }
        }

        return Optional.empty();
    }
}
