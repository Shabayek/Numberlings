# Numberlings keeps real behaviour off reflections — default optimize rules are fine.
# Keep the Room-generated implementations and our entities readable-by-database.
-keep class com.vividoai.numberlings.core.data.** { *; }

# Google Play Billing consumer rules already handle internal needed keeps.
