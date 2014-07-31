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
             :x        388
             :y        118
             :color    "#c0c0c0"
             :children {1 {:uid      1
                           :title    "communicate"
                           :x        601
                           :y        67
                           :color    "#67d7c4"
                           :children {3 {:uid      3
                                         :title    "letter"
                                         :x        717
                                         :y        25
                                         :color    "#67d7c4"
                                         :children {}}
                                      4 {:uid      4
                                         :title    "idea"
                                         :x        719
                                         :y        91
                                         :color    "#67d7c4"
                                         :children {}}
                                      5 {:uid      5
                                         :title    "speach"
                                         :x        710
                                         :y        59
                                         :color    "#67d7c4"
                                         :children {}}}}
                        7 {:uid      7
                           :title    "create"
                           :x        560
                           :y        251
                           :color    "#ebd95f"
                           :children {8 {:uid      8
                                         :title    "brain bloom"
                                         :x        650
                                         :y        251
                                         :color    "#ebd95f"
                                         :children {}}
                                      9 {:uid      9
                                         :title    "generate"
                                         :x        635
                                         :y        286
                                         :color    "#ebd95f"
                                         :children {}}
                                      10 {:uid      10
                                          :title    "apply"
                                          :x        635
                                          :y        219
                                          :color    "#ebd95f"
                                          :children {}}}}
                        11 {:uid      11
                            :title    "think"
                            :x        308
                            :y        231
                            :color    "#e68782"
                            :children {12 {:uid      12
                                           :title    "problem"
                                           :x        178
                                           :y        201
                                           :color    "#e68782"
                                           :children {}}
                                       13 {:uid      13
                                           :title    "idea"
                                           :x        172
                                           :y        313
                                           :color    "#e68782"
                                           :children {}}
                                       14 {:uid      14
                                           :title    "vision"
                                           :x        170
                                           :y        271
                                           :color    "#e68782"
                                           :children {}}
                                       15 {:uid      15
                                           :title    "organize"
                                           :x        175
                                           :y        237
                                           :color    "#e68782"
                                           :children {}}}}
                        16 {:uid      16
                            :title    "manage"
                            :x        253
                            :y        156
                            :color    "#56e304"
                            :children {17 {:uid      17
                                           :title    "team"
                                           :x        118
                                           :y        128
                                           :color    "#56e304"
                                           :children {}}
                                       18 {:uid      18
                                           :title    "meetings"
                                           :x        90
                                           :y        153
                                           :color    "#56e304"
                                           :children {}}
                                       19 {:uid      19
                                           :title    "time"
                                           :x        123
                                           :y        175
                                           :color    "#56e304"
                                           :children {}}}}
                        20 {:uid      20
                            :title    "prepare"
                            :x        295
                            :y        71
                            :color    "#FF08E1"
                            :children {21 {:uid      21
                                           :title    "project"
                                           :x        127
                                           :y        38
                                           :color    "#FF08E1"
                                           :children {}}
                                       22 {:uid      22
                                           :title    "training"
                                           :x        129
                                           :y        68
                                           :color    "#FF08E1"
                                           :children {}}
                                       23 {:uid      23
                                           :title    "trip"
                                           :x        129
                                           :y        99
                                           :color    "#FF08E1"
                                           :children {}}}}
                        24 {:uid      24
                            :title    "improve"
                            :x        608
                            :y        157
                            :color    "#64AEFF"
                            :children {25 {:uid      25
                                           :title    "memory"
                                           :x        734
                                           :y        124
                                           :color    "#64AEFF"
                                           :children {}}
                                       26 {:uid      26
                                           :title    "productivity"
                                           :x        731
                                           :y        194
                                           :color    "#64AEFF"
                                           :children {}}
                                       27 {:uid      27
                                           :title    "understanding"
                                           :x        736
                                           :y        159
                                           :color    "#64AEFF"
                                           :children {}}}}}}  
     }

    :tooltip
    ""
   }))
