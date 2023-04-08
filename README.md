# JynxWasm32

  This (2JYNX) reads a wasm file and produces a jynx file (JynxMacro library required to assemble).
It can also just parse a wasm file (PARSE).
It can be tested (TESTPARSE) with a w3c-1.0 testsuite file that contains 'module binary' sections.


The supported extensions to the mvp are

*	Sign extension instructions
*	Non-trapping float-to-int conversions
*	memory copy and memory fill
*	section 12
*	extended name section

## Usage

Usage: (version 0.1.3)

```
	2JYNX [options] wasm-file
```

  Options are:

*	--LEVEL            changes log-level
*	--CLASS_NAME_AS_IS stops changing first character of class name to upper case
*	--COMMENT          add wasm ops as comments to Jynx output
*	--NAME             class_name ; default name is module-name else filename without the .wasm extension
*	--PACKAGE          package name ; default is 'wasirun'
*	--START            set start method if wasm start not set. default is '_start' if it exists

```
	TESTPARSE [options] wast-file
		run w3c-1.0 testsuite file that contains 'module binary'
		e.g. [binary.wast in w3c-1.0 branch](https://github.com/WebAssembly/spec/blob/w3c-1.0/test/core/binary.wast)
```

  Options are:

*	--LEVEL            changes log-level

```
	PARSE [options] wasm-file
```

  Options are:

*	--LEVEL            changes log-level

## Notice

The source code contains some documentation snippets from 
[WebAssembly BinaryEncoding](https://github.com/WebAssembly/design/blob/main/BinaryEncoding.md)
