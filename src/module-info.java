module com.github.david32768.jynxwasm32 {
	requires java.logging;
	exports com.github.david32768.jynxwasm32;
}
// javac -p asmmods -d build\classes module-info.java
// jar --create --file JynxWasm32.jar --main-class com.github.david32768.jynxwasm32.Main --module-version 0.1 -C build\classes\ .
/*
.version V11
.source module-info.java
.define_module
.module com.github.david32768.jynxwasm32 0.1
.main com/github/david32768/jynxwasm32/Main
.exports com/github/david32768/jynxwasm32
.requires mandated java.base 11
.requires java.logging 11
.packages .array
  com/github/david32768/jynxwasm32
  jynxwasm32
  main
  parse
  util
  utility
  wasm
.end_array
.end_module
; */