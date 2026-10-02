# Add project-specific ProGuard rules here.

# Room entities — prevent R8 from stripping data classes
-keep class com.seemakit.field.Parcel { *; }
-keep class com.seemakit.field.Corner { *; }

# Keep Room DAO interface
-keep interface com.seemakit.field.SurveyDao { *; }

# Keep NMEA parser (referenced reflectively by some Bluetooth stacks)
-keep class com.seemakit.field.Nmea { *; }
