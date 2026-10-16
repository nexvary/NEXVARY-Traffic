package com.nexvary.trafficlab.core;

import java.util.*;

public class ExplainEngine {
    public static String explain(LabRequest r, boolean arabic){
        StringBuilder s=new StringBuilder(); if(arabic){ s.append("الطريقة ").append(r.method).append(": تحدد العملية المطلوبة من الخادم.\n"); s.append("المسار ").append(r.path).append(": المورد أو نقطة API المستهدفة.\n");
            for(var e:r.headers.entrySet()) s.append(e.getKey()).append(": ").append(explainHeaderAr(e.getKey())).append("\n"); s.append("Body: البيانات التي يرسلها العميل. لا ينبغي للخادم الوثوق بالقيم الحساسة القادمة من العميل دون تحقق.\n"); }
        else { s.append("Method ").append(r.method).append(": operation requested from the server.\nPath ").append(r.path).append(": target resource/API endpoint.\n"); for(var e:r.headers.entrySet()) s.append(e.getKey()).append(": ").append(explainHeaderEn(e.getKey())).append("\n"); s.append("Body: client-supplied data; sensitive values require server-side validation.\n"); }
        return s.toString();
    }
    private static String explainHeaderAr(String k){ String n=k.toLowerCase(Locale.ROOT); if(n.equals("content-type"))return "نوع تنسيق البيانات المرسلة."; if(n.equals("authorization"))return "بيانات اعتماد ويجب إخفاؤها في العروض."; if(n.equals("user-agent"))return "هوية العميل البرمجية."; if(n.equals("host"))return "اسم المضيف المستهدف."; return "ترويسة HTTP إضافية."; }
    private static String explainHeaderEn(String k){ String n=k.toLowerCase(Locale.ROOT); if(n.equals("content-type"))return "format of the request payload."; if(n.equals("authorization"))return "credential material; redact in presentations."; if(n.equals("user-agent"))return "software client identity."; if(n.equals("host"))return "target host name."; return "additional HTTP metadata."; }
}
