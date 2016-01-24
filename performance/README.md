
=== Jan 3 Test 001 ===

commit: dd1443fe541389f8c38f28002240cb24a587b485

Proxy TCP to UDP. Two SIPps pushing TCP over one connection each. One SIPp acting as UAS, UDP only.

No JVM parameter changes whatsoever. Just default everything.

800 CPS is ok but around 1000-1100:ish things went south...

CPU: 124%
GC: only young generation collections (see details below)

SIPp UAC:
sipp -sn uac -t t1 -max_socket 200 -r 1 -rsa 127.0.0.1:5060  127.0.0.1:5080 

SIPp UAS:
sipp -sn uas -p 5080

jstat: 
 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT   
1024.0 1024.0  0.0   160.0  124416.0 94171.5   171008.0   42728.4   15488.0 15161.7 1920.0 1786.5   1699    2.195   0      0.000    2.195
1024.0 1024.0  0.0   608.0  116736.0   0.0     171008.0   42792.4   15488.0 15161.7 1920.0 1786.5   1701    2.200   0      0.000    2.200
512.0  1024.0 448.0   0.0   113152.0 15758.2   171008.0   42824.4   15488.0 15161.7 1920.0 1786.5   1702    2.206   0      0.000    2.206
1024.0 1024.0  0.0   224.0  109568.0 46773.6   171008.0   42856.4   15488.0 15161.7 1920.0 1786.5   1703    2.209   0      0.000    2.209
512.0  1024.0 448.0   0.0   106496.0 59534.4   171008.0   42888.4   15488.0 15161.7 1920.0 1786.5   1704    2.212   0      0.000    2.212
1024.0 1024.0  0.0   416.0  103424.0 61231.6   171008.0   42912.4   15488.0 15161.7 1920.0 1786.5   1705    2.216   0      0.000    2.216
512.0  1024.0 480.0   0.0   100352.0 67258.8   171008.0   42952.4   15488.0 15161.7 1920.0 1786.5   1706    2.220   0      0.000    2.220
1024.0 1024.0  0.0   192.0  97792.0  65562.0   171008.0   42984.4   15488.0 15161.7 1920.0 1786.5   1707    2.224   0      0.000    2.224

=== Jan 3 Test 002 ===

Same as Test 01 but with 6 SIPp UACs instead of two.

Def went better. 1600 CPS across those 6 SIPps didn't seem to be an issue. At 2400 CPS things went south. Seems like some SIPp instances started to misbehave, not sure why. Conclusion for now though is that we should be able to go beyond 800 CPS as long as we don't do more per TCP connection.

Idea: add a limit to how much traffic we are accepting across a single TCP connection. Perhaps even start to push back? Should probably check the netty writability change notifications etc.

 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT   
512.0  512.0  288.0   0.0   116224.0 28152.2   171008.0   42089.7   15744.0 15388.7 1920.0 1818.0   1420    2.923   0      0.000    2.923
512.0  512.0  288.0   0.0   116224.0 28152.2   171008.0   42089.7   15744.0 15388.7 1920.0 1818.0   1420    2.923   0      0.000    2.923
512.0  512.0  288.0   0.0   116224.0 28152.2   171008.0   42089.7   15744.0 15388.7 1920.0 1818.0   1420    2.923   0      0.000    2.923
512.0  512.0  288.0   0.0   116224.0 28152.2   171008.0   42089.7   15744.0 15388.7 1920.0 1818.0   1420    2.923   0      0.000    2.923
512.0  512.0  288.0   0.0   116224.0 28152.2   171008.0   42089.7   15744.0 15388.7 1920.0 1818.0   1420    2.923   0      0.000    2.923
512.0  512.0  288.0   0.0   116224.0 28152.2   171008.0   42089.7   15744.0 15388.7 1920.0 1818.0   1420    2.923   0      0.000    2.923
512.0  512.0  288.0   0.0   116224.0 28152.2   171008.0   42089.7   15744.0 15388.7 1920.0 1818.0   1420    2.923   0      0.000    2.923

=== Jan 3 Test 003 ===

previous commit: dd1443fe541389f8c38f28002240cb24a587b485
this commit: 606d226abeef51e58e75ca9638b2293ee1abbf41

Exacte same as 002 but made a small change to the code. Do a diff of the commits to see what.

 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT   
512.0  512.0   0.0   160.0  70656.0  28293.0   171008.0   37280.6   15744.0 15324.9 1920.0 1818.5   1369    1.787   0      0.000    1.787
512.0  512.0   0.0   160.0  70656.0  30044.9   171008.0   37280.6   15744.0 15324.9 1920.0 1818.5   1369    1.787   0      0.000    1.787
512.0  512.0   0.0   160.0  70656.0  30044.9   171008.0   37280.6   15744.0 15324.9 1920.0 1818.5   1369    1.787   0      0.000    1.787
512.0  512.0   0.0   160.0  70656.0  30044.9   171008.0   37280.6   15744.0 15324.9 1920.0 1818.5   1369    1.787   0      0.000    1.787
512.0  512.0   0.0   160.0  70656.0  30044.9   171008.0   37280.6   15744.0 15324.9 1920.0 1818.5   1369    1.787   0      0.000    1.787
512.0  512.0   0.0   160.0  70656.0  30044.9   171008.0   37280.6   15744.0 15324.9 1920.0 1818.5   1369    1.787   0      0.000    1.787
512.0  512.0   0.0   160.0  70656.0  30628.9   171008.0   37280.6   15744.0 15324.9 1920.0 1818.5   1369    1.787   0      0.000    1.787
512.0  512.0   0.0   160.0  70656.0  31335.5   171008.0   37280.6   15744.0 15324.9 1920.0 1818.5   1369    1.787   0      0.000    1.787
512.0  512.0   0.0   160.0  70656.0  31335.5   171008.0   37280.6   15744.0 15324.9 1920.0 1818.5   1369    1.787   0      0.000    1.787
512.0  512.0   0.0   160.0  70656.0  32503.5   171008.0   37280.6   15744.0 15324.9 1920.0 1818.5   1369    1.787   0      0.000    1.787

=== Jan 3 Test 004 ===

Went back to a single SIPp instance to see if the above commits made any difference for it and yep, it did!

1500 CPS, which was sustained for about 10 min, showed no issue whatsoever. CPU at 87%. No dropped calls, retransmissions or anything like that reported by SIPp so really good actually. And 99.999% of the calls had a avg response of less than 10ms

Cranked it up to 2030 CPS across this single TCP connection and still amazing! CPU incrased to 110-120% but still no issues reported by SIPp. As in, no retransmissions or anything. This load was sustained for about 5 min.... hmmm... a few retransmissions after about 5 min. Not sure why. jstat doesn't seem to indicate that a full GC took place either. I did change swap Workspace, which will take CPU i guess so perhaps that interferred with the test. Since then, the test is just going strong.

Cranked it up to 2500 CPS and now it started to break apart. 6 timeouts after a few min of this load. 2100 retransmissions and CPU is up to 125%. Think we hit the limit for one TCP connection can handle with the current code. And again, we haven't actually changed any of the GC settings or anything. Everything is default.

Hmmm... after that one burst of timeouts it calmed down and is n ow handling the 2500 CPS just fine.

Cranked it up to 3100 CPS. Lots of retransmissions right away. CPU is up to 170% for the java process. Lots of timeouts as well and SIPP UAC isn't able to push that many calls other than in a very spiky fashion. Ok, for now, 3100 CPS is too much. 2000 is def safe. 2500 may be ok.


=== Jan 3 Test 005 ===

Exact same as 004 but with the difference that we changed the max allowed message size to be:
1024 for initial line
2048 for header
1024 for body
so a total of 4k where the previous one had:

1024 for initial line
4096 for header
4096 for body

so 11k total. The reason is that we are still slightly inefficient with copying memory a little too frequently so wanted to see how big of an impact changing these values actually has. Still a major copy happening when I write the message out so changing that one should make a difference too. Unfortunately I haven't kept track of memory in the other tests. Actually, should be in the GC stats...

1500 CPS sustained. Similar result as in 004 but perhaps slightly lower CPU of 75-82%. Was 87 before.

2030 CPS sustained. Some retransmissions, which the previous one had as well. COU between 98-110% so perhaps slightly lower again...

2500 CPS sustained. Same as previous. CPU is 130-135%, which is slightly higher. Otherwise everything is the same...

Conclusion: may be a slight improvement but not earth shattering!


 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT   
512.0  512.0  512.0   0.0   51712.0  19707.5   171008.0   13312.9   15488.0 15200.6 1920.0 1789.9   5064    3.399   0      0.000    3.399
512.0  512.0   96.0   0.0   58880.0  13864.1   171008.0   13376.9   15488.0 15200.6 1920.0 1789.9   5068    3.403   0      0.000    3.403
512.0  512.0   96.0   0.0   54784.0  24084.8   171008.0   13456.9   15488.0 15200.6 1920.0 1789.9   5072    3.406   0      0.000    3.406
512.0  512.0   96.0   0.0   50688.0  36955.6   171008.0   13544.9   15488.0 15200.6 1920.0 1789.9   5076    3.408   0      0.000    3.408
512.0  512.0   0.0    96.0  46592.0  18609.7   171008.0   13640.9   15488.0 15200.6 1920.0 1789.9   5081    3.411   0      0.000    3.411
512.0  512.0   64.0   0.0   44032.0  14094.2   171008.0   13728.9   15488.0 15200.6 1920.0 1789.9   5086    3.415   0      0.000    3.415
512.0  512.0   0.0    96.0  41472.0  24069.1   171008.0   13816.9   15488.0 15200.6 1920.0 1789.9   5091    3.418   0      0.000    3.418
512.0  512.0   0.0    96.0  38400.0   7250.1   171008.0   13920.9   15488.0 15200.6 1920.0 1789.9   5097    3.423   0      0.000    3.423
512.0  512.0   0.0    96.0  35328.0   9548.5   171008.0   14016.9   15488.0 15200.6 1920.0 1789.9   5103    3.427   0      0.000    3.427
512.0  512.0   96.0   0.0   31744.0    0.0     171008.0   14112.9   15488.0 15200.6 1920.0 1789.9   5110    3.432   0      0.000    3.432

=== Jan 3 Test 006 ===

Changed so that we do not copy the memory again in the SipMessageStreamEncoder where we turn the SIP message into a byte array. Didn't seem to do a huge difference actually but still is a good idea though... See commit for the diff...


 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT   
512.0  512.0  128.0   0.0   53760.0  12362.8   171008.0   27837.1   15488.0 15198.4 1920.0 1789.8   1992    1.435   0      0.000    1.435
512.0  512.0  128.0   0.0   53760.0  12362.8   171008.0   27837.1   15488.0 15198.4 1920.0 1789.8   1992    1.435   0      0.000    1.435
512.0  512.0  128.0   0.0   53760.0  12362.8   171008.0   27837.1   15488.0 15198.4 1920.0 1789.8   1992    1.435   0      0.000    1.435
512.0  512.0  128.0   0.0   53760.0  12362.8   171008.0   27837.1   15488.0 15198.4 1920.0 1789.8   1992    1.435   0      0.000    1.435
512.0  512.0  128.0   0.0   53760.0  12362.8   171008.0   27837.1   15488.0 15198.4 1920.0 1789.8   1992    1.435   0      0.000    1.435
512.0  512.0  128.0   0.0   53760.0  12362.8   171008.0   27837.1   15488.0 15198.4 1920.0 1789.8   1992    1.435   0      0.000    1.435
512.0  512.0  128.0   0.0   53760.0  12362.8   171008.0   27837.1   15488.0 15198.4 1920.0 1789.8   1992    1.435   0      0.000    1.435
512.0  512.0  128.0   0.0   53760.0  12362.8   171008.0   27837.1   15488.0 15198.4 1920.0 1789.8   1992    1.435   0      0.000    1.435
512.0  512.0  128.0   0.0   53760.0  12362.8   171008.0   27837.1   15488.0 15198.4 1920.0 1789.8   1992    1.435   0      0.000    1.435
512.0  512.0  128.0   0.0   53760.0  13975.8   171008.0   27837.1   15488.0 15198.4 1920.0 1789.8   1992    1.435   0      0.000    1.435

=== Jan 10 ====

Tested against: 3336f19fa9a0ecc0e14afe55f38c7cd1a6cdbb50
and against a unrelased pkts.io: c31cacf10d8c1608e91490202147313aeffbe0ea

Pure UAS 5000 CPS was not an issue. Didn't really keep track of numbers but SIPp's numbers were perfect. Same for both UDP and TCP. Started to break down around 5500.

For acting as a proxy (running proxy example 004) we could do 2000 CPS no problem (tcp & udp had the same results). 2500 CPS was somewhat ok but started to break down. 3000 CPS not a chance... I guess that is consistent with the pure UAS numbers since the UAS scenario is doing half the number of messages...

Overall, pretty good.

=== Jan 23 ===

Running Proxy Example 005, which has Flow support.

Pure UDP to UDP 1000 CPS was no issue. 1500 CPS started to become an issue and 2000 not a chance. Need to do some profiling to see why the big diff.
