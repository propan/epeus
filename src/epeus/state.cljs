(ns epeus.state
  (:require [epeus.utils :refer [now]]))

(def app-state
  (atom
   {:graph
    {}

    :main
    {
     :modified
     (now)
     :items {:uid      0
             :root?    true
             :title    "Mind Maps"
             :x        398
             :y        199
             :color    "#c0c0c0"
             :children {1 {:uid      1
                           :title    "communicate"
                           :x        531
                           :y        122
                           :color    "#67d7c4"
                           :children {3 {:uid   3
                                         :title "letter"
                                         :x 655
                                         :y 92
                                         :color "#67d7c4"
                                         :children {}}
                                      4 {:uid      4
                                         :title    "idea"
                                         :x        656
                                         :y        153
                                         :color    "#67d7c4"
                                         :children {}}
                                      5 {:uid      5
                                         :title    "speach"
                                         :x        601
                                         :y        182
                                         :color    "#67d7c4"
                                         :children {}}}}
                        7 {:uid      7
                           :title    "create"
                           :x        562
                           :y        257
                           :color    "#ebd95f"
                           :children {8  {:uid      8
                                          :title    "brain bloom"
                                          :x        644
                                          :y        241
                                          :color    "#ebd95f"
                                          :children {}}
                                      9  {:uid      9
                                          :title    "generate"
                                          :x        631
                                          :y        318
                                          :color    "#ebd95f"
                                          :children {}}
                                      10 {:uid      10
                                          :title    "apply"
                                          :x        469
                                          :y        320
                                          :color    "#ebd95f"
                                          :children {}}}}
                        11 {:uid      11
                            :title    "think"
                            :x        335
                            :y        322
                            :color    "#e68782"
                            :children {12 {:uid      12
                                           :title    "problem"
                                           :x        243
                                           :y        293
                                           :color    "#e68782"
                                           :children {}}
                                       13 {:uid      13
                                           :title    "idea"
                                           :x        250
                                           :y        339
                                           :color    "#e68782"
                                           :children {}}
                                       14 {:uid      14
                                           :title    "vision"
                                           :x        267
                                           :y        390
                                           :color    "#e68782"
                                           :children {}}
                                       15 {:uid      15
                                           :title    "organize"
                                           :x        390
                                           :y        376
                                           :color    "#e68782"
                                           :children {}}}}
                        16 {:uid      16
                            :title    "manage"
                            :x        287
                            :y        221
                            :color    "#56e304"
                            :children {17  {:uid      17
                                            :title    "team"
                                            :x        210
                                            :y        165
                                            :color    "#56e304"
                                            :children {}}
                                       18  {:uid      18
                                            :title    "meetings"
                                            :x        176
                                            :y        208
                                            :color    "#56e304"
                                            :children {}}
                                       19 {:uid      19
                                           :title    "time"
                                           :x        193
                                           :y        279
                                           :color    "#56e304"
                                           :children {}}}}
                        20 {:uid      20
                            :title    "prepare"
                            :x        235
                            :y        128
                            :color    "#FF08E1"
                            :children {21 {:uid      21
                                           :title    "project"
                                           :x        171
                                           :y        51
                                           :color    "#FF08E1"
                                           :children {}}
                                       22 {:uid      22
                                           :title    "training"
                                           :x        133
                                           :y        113
                                           :color    "#FF08E1"
                                           :children {}}
                                       23 {:uid      23
                                           :title    "trip"
                                           :x        124
                                           :y        153
                                           :color    "#FF08E1"
                                           :children {}}}}
                        24 {:uid      24
                            :title    "improve"
                            :x        357
                            :y        132
                            :color    "#64AEFF"
                            :children {25 {:uid      25
                                           :title    "memory"
                                           :x        244
                                           :y        72
                                           :color    "#64AEFF"
                                           :children {}}
                                       26 {:uid      26
                                           :title    "productivity"
                                           :x        344
                                           :y        51
                                           :color    "#64AEFF"
                                           :children {}}
                                       27 {:uid      27
                                           :title    "understanding"
                                           :x        467
                                           :y        90
                                           :color    "#64AEFF"
                                           :children {}}}}}}
     }

    :tooltip
    ""
   }))
