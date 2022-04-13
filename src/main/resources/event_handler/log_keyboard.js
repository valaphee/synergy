/*
 * Copyright (c) 2022, Valaphee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var keys = [
    /*   *  0            1               2           3          4             5         6        7          8         9        A        B          C           D          E        F        *   */
    /* 0 */ ""         , ""            , ""        , ""       , ""          , ""      , ""     , ""       , "[BS]"  , "[TAB]", ""     , ""       , "[DEL]"   , "[ENTER]", ""     , ""     ,/* 0 */
    /* 1 */ "[SHIFT]"  , "[CTRL]"      , "[MENU]"  , "[PAUSE]", "[CAPSLOCK]", ""      , ""     , ""       , ""      , ""     , ""     , "[ESC]"  , ""        , ""       , ""     , ""     ,/* 1 */
    /* 2 */ " "        , "[PGUP]"      , "[PGDOWN]", "[END]"  , "[HOME]"    , "[LEFT]", "[UP]" , "[RIGHT]", "[DOWN]", ""     , ""     , ""       , "[PRTSCR]", "[INS]"  , "[DEL]", ""     ,/* 2 */
    /* 3 */ "0"        , "1"           , "2"       , "3"      , "4"         , "5"     , "6"    , "7"      , "8"     , "9"    , ""     , ""       , ""        , ""       , ""     , ""     ,/* 3 */
    /* 4 */ ""         , "A"           , "B"       , "C"      , "D"         , "E"     , "F"    , "G"      , "H"     , "I"    , "J"    , "K"      , "L"       , "M"      , "N"    , "O"    ,/* 4 */
    /* 5 */ "P"        , "Q"           , "R"       , "S"      , "T"         , "U"     , "V"    , "W"      , "X"     , "Y"    , "Z"    , "[LMETA]", "[RMETA]" , ""       , ""     , ""     ,/* 5 */
    /* 6 */ "0"        , "1"           , "2"       , "3"      , "4"         , "5"     , "6"    , "7"      , "8"     , "9"    , "*"    , "+"      , ""        , "-"      , ","    , "/"    ,/* 6 */
    /* 7 */ "[F1]"     , "[F2]"        , "[F3]"    , "[F4]"   , "[F5]"      , "[F6]"  , "[F7]" , "[F8]"   , "[F9]"  , "[F10]", "[F11]", "[F12]"  , "[F13]"   , "[F14]"  , "[F15]", "[F16]",/* 7 */
    /* 8 */ "[F17]"    , "[F18]"       , "[F19]"   , "[F20]"  , "[F21]"     , "[F22]" , "[F23]", "[F24]"  , ""      , ""     , ""     , ""       , ""        , ""       , ""     , ""     ,/* 8 */
    /* 9 */ "[NUMLOCK]", "[SCROLLLOCK]", ""        , ""       , ""          ,  ""     , ""     , ""       , ""      , ""     , ""     , ""       , ""        , ""       , ""     , ""     ,/* 9 */
    /* A */ "[LSHIFT]" , "[RSHIFT]"    , "[LCTRL]" , "[RCTRL]", ""          ,  ""     , ""     , ""       , ""      , ""     , ""     , ""       , ""        , ""       , ""     , ""     ,/* A */
    /* B */ ""         , ""            , ""        , ""       , ""          ,  ""     , ""     , ""       , ""      , ""     , ""     , ""       , ""        , ""       , ""     , ""     ,/* B */
    /* C */ ""         , ""            , ""        , ""       , ""          ,  ""     , ""     , ""       , ""      , ""     , ""     , ""       , ""        , ""       , ""     , ""     ,/* C */
    /* D */ ""         , ""            , ""        , ""       , ""          ,  ""     , ""     , ""       , ""      , ""     , ""     , ""       , ""        , ""       , ""     , ""     ,/* D */
    /* E */ ""         , ""            , ""        , ""       , ""          ,  ""     , ""     , ""       , ""      , ""     , ""     , ""       , ""        , ""       , ""     , ""     ,/* E */
    /* F */ ""         , ""            , ""        , ""       , ""          ,  ""     , ""     , ""       , ""      , ""     , ""     , ""       , ""        , ""       , ""     , ""     ,/* F */
    /*   *  0            1               2           3          4             5         6        7          8         9        A        B          C           D          E        F        *   */
];

var pressedKeys = new Set();

(function (action, event) {
    if (event.type === "keyboard") {
        if (event.event === 1) {
            pressedKeys.add(event.key_code)
            console.log(keys[event.key_code])
        } else if (event.event === 2) {
            pressedKeys.delete(event.key_code)
        }
    }
})
