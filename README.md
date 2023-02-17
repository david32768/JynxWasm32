# JynxWasm32

  This reads a wasm file and produces a jynx file (JynxMacro library required to assemble)

The supported extensions to the mvp are

*	Sign extension instructions
*	Non-trapping float-to-int conversions
*	memory copy and memory fill
*	section 12
*	extended name section

## Usage

Usage: (version 0.1.1)
    2jynx {options} wasm-file
        convert to a jynx file

Options are:

*	--DEBUG changes log-level to FINEST
*	--CLASS_NAME_AS_IS stops changing first character of class name to upper case
*	--COMMENT add wasm ops as comments to Jynx output
*	--PACKAGE add package name
*	--NAME class_name  ; default is module-name else filename without the .wasm extension

    testparse [--LEVEL <log-level>] wast-file
        run w3c-1.0 testsuite file that contains 'module binary'
	e.g. [binary.wast in w3c-1.0 branch](https://github.com/WebAssembly/spec/blob/w3c-1.0/test/core/binary.wast)

## Notice

The source code contains some documentation snippets from 
[WebAssembly BinaryEncoding](https://github.com/WebAssembly/design/blob/main/BinaryEncoding.md)
