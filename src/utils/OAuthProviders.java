package utils;

import soot.Body;
import soot.SootClass;
import soot.Unit;
import soot.jimple.InvokeExpr;

public class OAuthProviders {
    public static boolean detectGoogle(String className, String methodName, String methodClassName) {
        if (methodName.equals("getClient")) {
            if (methodClassName.contains("com.google.android.gms.auth.api.signin.GoogleSignIn")) {
                if (!className.contains("com.google")) {
                    if (config.Constants.debug) {
                        System.out.println("Google in " + className + ": " + methodClassName + "." + methodName);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean detectGoogleMigrate(Body b, Unit u, String className, InvokeExpr invokeExpr) {
        String methodName = invokeExpr.getMethod().getName();
        String methodClassName = invokeExpr.getMethod().getDeclaringClass().getName();

        if (methodName.equals("addApi") || methodName.equals("addApiIfAvailable")) {
            if (methodClassName.contains("com.google.android.gms.common.api.GoogleApiClient$Builder")) {
                if (!className.contains("com.google")) {
                    String arg = invokeExpr.getArg(0).toString();
                    Unit curUnit = u;
                    do {
                        curUnit = b.getUnits().getPredOf(curUnit);
                        if (curUnit.getDefBoxes().size() != 0 && curUnit.getDefBoxes().get(0).getValue().toString().equals(arg)) {
                            if (curUnit.getUseBoxes().size() != 0 && curUnit.getUseBoxes().get(0).getValue().toString().contains("<com.google.android.gms.auth.api.Auth: com.google.android.gms.common.api.Api GOOGLE_SIGN_IN_API>")) {
                                if (config.Constants.debug) {
                                    System.out.println("Google (Migrate) in " + className + ": " + methodClassName + "." + methodName);
                                }
                                return true;
                            } else {
                                return false;
                            }
                        }
                    } while (b.getUnits().getPredOf(curUnit) != null);
                }
            }
        }
        return false;
    }

    public static boolean detectGoogleV1(Body b, Unit u, String className, InvokeExpr invokeExpr) {
        String methodName = invokeExpr.getMethod().getName();
        String methodClassName = invokeExpr.getMethod().getDeclaringClass().getName();

        if (methodName.equals("addApi") || methodName.equals("addApiIfAvailable")) {
            if (methodClassName.contains("com.google.android.gms.common.api.GoogleApiClient$Builder")) {
                if (!className.contains("com.google")) {
                    String arg = invokeExpr.getArg(0).toString();
                    Unit curUnit = u;
                    do {
                        curUnit = b.getUnits().getPredOf(curUnit);
                        if (curUnit.getDefBoxes().size() != 0 && curUnit.getDefBoxes().get(0).getValue().toString().equals(arg)) {
                            if (curUnit.getUseBoxes().size() != 0 && curUnit.getUseBoxes().get(0).getValue().toString().contains("<com.google.android.gms.plus.Plus: com.google.android.gms.common.api.Api API>")) {
                                if (config.Constants.debug) {
                                    System.out.println("Google (V1) in " + className + ": " + methodClassName + "." + methodName);
                                }
                                return true;
                            } else {
                                return false;
                            }
                        }
                    } while (b.getUnits().getPredOf(curUnit) != null);
                }
            }
        }
        return false;
    }

    public static boolean detectFacebook(String className, String methodName, String methodClassName) {
        if (methodName.equals("registerCallback")) {
            if (methodClassName.contains("com.facebook.login.LoginManager") || methodClassName.contains("com.facebook.login.LoginButton")) {
                if (!className.contains("com.facebook")) {
                    if (config.Constants.debug) {
                        System.out.println("Facebook in " + className + ": " + methodClassName + "." + methodName);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean detectFacebookV3(String className, String methodName, String methodClassName) {
        if (methodName.equals("addCallback")) {
            if (methodClassName.contains("com.facebook.Session")) {
                if (!className.contains("com.facebook")) {
                    if (config.Constants.debug) {
                        System.out.println("Facebook (V3) in " + className + ": " + methodClassName + "." + methodName);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean detectTwitterAuthentication(String className, String methodName, String methodClassName) {
        if (methodName.equals("setCallback")) {
            if (methodClassName.contains("com.twitter.sdk.android.core.identity.TwitterLoginButton")) {
                if (!className.contains("com.twitter")) {
                    if (config.Constants.debug) {
                        System.out.println("Twitter (Authentication) in " + className + ": " + methodClassName + "." + methodName);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean detectTwitterAuthorization(String className, String methodName, String methodClassName) {
        if (methodName.equals("authorize")) {
            if (methodClassName.contains("com.twitter.sdk.android.core.identity.TwitterAuthClient")) {
                if (!className.contains("com.twitter")) {
                    if (config.Constants.debug) {
                        System.out.println("Twitter (Authorization) in " + className + ": " + methodClassName + "." + methodName);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean detectTwitter4J(String className, String methodName, String methodClassName) {
        if (methodName.equals("getOAuthRequestToken")) {
            if (methodClassName.contains("twitter4j.auth.OAuthSupport")) {
                if (!className.contains("twitter4j")) {
                    if (config.Constants.debug) {
                        System.out.println("Twitter4J in " + className + ": " + methodClassName + "." + methodName);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean detectVK(String className, String methodName, String methodClassName) {
        if (methodName.equals("initialize")) {
            if (methodClassName.contains("com.vk.sdk.VKSdk")) {
                if (!className.contains("com.vk")) {
                    if (config.Constants.debug) {
                        System.out.println("VK in " + className + ": " + methodClassName + "." + methodName);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean detectLinkedIn(String className, String methodName, String methodClassName) {
        if (methodName.equals("init")) {
            if (methodClassName.contains("com.linkedin.platform.LISessionManager")) {
                if (!className.contains("com.linkedin")) {
                    if (config.Constants.debug) {
                        System.out.println("LinkedIn in " + className + ": " + methodClassName + "." + methodName);
                    }
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean detectAmazon(String className, String methodName, String methodClassName) {
        if (methodName.equals("registerListener")) {
            if (methodClassName.contains("com.amazon.identity.auth.device.api.workflow.RequestContext")) {
                if (!className.contains("com.amazon")) {
                    if (config.Constants.debug) {
                        System.out.println("Amazon in " + className + ": " + methodClassName + "." + methodName);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean detectFoursquare(String className, String methodName, String methodClassName) {
        if (methodName.equals("getConnectIntent")) {
            if (methodClassName.contains("com.foursquare.android.nativeoauth.FoursquareOAuth")) {
                if (!className.contains("com.foursquare")) {
                    if (config.Constants.debug) {
                        System.out.println("Foursquare in " + className + ": " + methodClassName + "." + methodName);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
