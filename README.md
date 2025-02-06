THE INDOOR TRACKING SYSYTEM IS NOT ONLY A BASIC TRACKING SYSTEM WHICH TELLS THE LOCATION OF THE USER ON A 2D MAP BUT IT ALSO TELLS ABOUT THE DETAILS OF THE LOGITUDES, LATITUDES, FLOOR AND ROOM NUMBER OF THE BUILDING IN WHICH THE USER MIGHT BE PRESENT.
THIS SYSTEM IS SOLELY DEPENDENT ON THE *BLE*(BLUETOOTH BEACON) TECHNOLOGY AND GPS TRACKING SYSTEM. NOW THIS IS HOW THIS TECHNOLOGY WORKS- LETS SAY OUR AIM IS TO GET THE OUTPUT I.E REAL TIME INDOOR TRACKING OF AN INDIVIDUAL OUT OF OUR MOBILE PHONES
ITSELF WHICH CAN BE EASILY ARRANGED. NOW THERE ARE 2 ANDROID APPS NAMED AS *ANDROID-APP-1* AND *ANDROID-APP-2*. THE DEVICES WITH THE *ANDROID-APP-1* IS PLACED ON DIFFERENT FLOORS AND ARE GIVEN UNIQUE ID'S. NOW THESE DEVICES GIVE OUT BLUTOOTH FREQUENCIES
ON EACH FLOOR. LETS SAY THAT THE USER(WHO WANTS HIS LOCATION TO BE TRACKED) HAS *ANDROID-APP-2* IN HIS DEVICE. NOW HE IS IN THE BUILDING WHERE THE APP WILL CATCH THE BLUETOOTH FREQUENCIES GIVEN OUT BY *ANDROID-APP-1*. IF THE USER IS ON THE FIRST FLOOR THE APP WILL
CATCH THE BLUTOOTH FREQEUNCY OF THE DEVICE PLACED ON THE FIRST FLOOR AND WILL SEND THE DATE ABOUT FLOOR DETAILS, LATS AND LONGS TO THE WEB APP VIA ESTABLISHED WEB SOCKETS DEVELOPED USING FAST API. THE WEB APPLICATION IS DESIGNED IN SUCH A WAY THAT IT WILL INTERPRET
ALL THE DATA RECEIVED BY THE *ANDROID-APP-2* AND WILL SHOW THE LOCATION ON A 2D MAP AS WELL AS THE DATA ABOUT LONGS AND LATS, FLOOR DETAILS WILLL BE SAVED IN THE FORM OF CSV FILE WHICH CAN BE RETRIEVED USING DESIGNATED APP-ROUTES. THE *ANDROID-APP-2* IS 
PROGRAMMED TO SEND THE UPDATED LOCATION EVERY 0.5 SECONDS TO THE WEB APP SO AS TO GET THE PRECISE AND ACCURATE LOCATION IN REAL TIME. THE SECOND APP COLLECTS LATS AND LONGS DATA UPTO 12 SIGNIFICANT FIGURES WHICH MEANS THE CORDINATES ARE SO ACCURATE THAT IF THE USER
MOVES EVEN 10CM HIS LOCATION WILL BNE UPDATED. *ANDROID-APP-1* ONLY EXISTS TO MARK THE FLOORS IN THE BUILDING WHILE THE ROLE OF THE SECOND APP IS TO CATCH THE BLUTOOTH FREQUENCIES, GPS CORDINATES AND SEND THE DATA TO THE WEB APP. THE UNIQUE IDS MUST BE SET BEFOREHAND IN 
THE *ANDROID-APP-2* AS IT WILL ONLY CATCH THE PRESET FREQUENCIES TO PREVENT ANY INACCURACIES. THE ROLE OF THE WEB APP IS ONLY TO VISUALISE THE RECEIEVED DATA AND SAVE THE DATA IN A CSV FILE.



TO MAKE THIS MODEL WORK, IT IS ASSUMED THAT THERE IS A BASIC BUILDING WITH MULTIPLE FLOORS AND ALL THEM ARE EMPTY!!!
