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
             :x        369
             :y        106
             :color    "#c0c0c0"
             :children {1 {:uid      1
                           :title    "communicate"
                           :x        529
                           :y        69
                           :color    "#67d7c4"
                           :children {3 {:uid      3
                                         :title    "letter"
                                         :x        642
                                         :y        35
                                         :color    "#67d7c4"
                                         :children {}}
                                      4 {:uid      4
                                         :title    "idea"
                                         :x        642
                                         :y        61
                                         :color    "#67d7c4"
                                         :children {}}
                                      5 {:uid      5
                                         :title    "speach"
                                         :x        641
                                         :y        86
                                         :color    "#67d7c4"
                                         :children {}}}}
                        7 {:uid      7
                           :title    "create"
                           :x        528
                           :y        222
                           :color    "#ebd95f"
                           :children {8 {:uid      8
                                         :title    "brain bloom"
                                         :x        587
                                         :y        241
                                         :color    "#ebd95f"
                                         :children {}}
                                      9 {:uid      9
                                         :title    "generate"
                                         :x        540
                                         :y        267
                                         :color    "#ebd95f"
                                         :children {}}
                                      10 {:uid      10
                                          :title    "apply"
                                          :x        444
                                          :y        260
                                          :color    "#ebd95f"
                                          :children {}}}}
                        11 {:uid      11
                            :title    "think"
                            :x        215
                            :y        215
                            :color    "#e68782"
                            :children {12 {:uid      12
                                           :title    "problem"
                                           :x        89
                                           :y        191
                                           :color    "#e68782"
                                           :children {}}
                                       13 {:uid      13
                                           :title    "idea"
                                           :x        121
                                           :y        220
                                           :color    "#e68782"
                                           :children {}}
                                       14 {:uid      14
                                           :title    "vision"
                                           :x        110
                                           :y        256
                                           :color    "#e68782"
                                           :children {}}
                                       15 {:uid      15
                                           :title    "organize"
                                           :x        250
                                           :y        258
                                           :color    "#e68782"
                                           :children {}}}}
                        16 {:uid      16
                            :title    "manage"
                            :x        205
                            :y        135
                            :color    "#56e304"
                            :children {17 {:uid      17
                                           :title    "team"
                                           :x        70
                                           :y        107
                                           :color    "#56e304"
                                           :children {}}
                                       18 {:uid      18
                                           :title    "meetings"
                                           :x        42
                                           :y        132
                                           :color    "#56e304"
                                           :children {}}
                                       19 {:uid      19
                                           :title    "time"
                                           :x        75
                                           :y        154
                                           :color    "#56e304"
                                           :children {}}}}
                        20 {:uid      20
                            :title    "prepare"
                            :x        304
                            :y        69
                            :color    "#FF08E1"
                            :children {21 {:uid      21
                                           :title    "project"
                                           :x        136
                                           :y        36
                                           :color    "#FF08E1"
                                           :children {}}
                                       22 {:uid      22
                                           :title    "training"
                                           :x        138
                                           :y        66
                                           :color    "#FF08E1"
                                           :children {}}
                                       23 {:uid      23
                                           :title    "trip"
                                           :x        167
                                           :y        90
                                           :color    "#FF08E1"
                                           :children {}}}}
                        24 {:uid      24
                            :title    "improve"
                            :x        539
                            :y        151
                            :color    "#64AEFF"
                            :children {25 {:uid      25
                                           :title    "memory"
                                           :x        629
                                           :y        156
                                           :color    "#64AEFF"
                                           :children {}}
                                       26 {:uid      26
                                           :title    "productivity"
                                           :x        629
                                           :y        127
                                           :color    "#64AEFF"
                                           :children {}}
                                       27 {:uid      27
                                           :title    "understanding"
                                           :x        631
                                           :y        185
                                           :color    "#64AEFF"
                                           :children {}}}}}}  
     }

    :tooltip
    ""
   }))
