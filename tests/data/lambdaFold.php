<?php

// DON'T FOLD

// syntax errors prevent folding
array_map(funcion () {}, []);
array_map(function ( {}, []);
array_map(function ( }, []);
array_map(function ) }, []);
array_map(function () use () {}, []);
array_map(function () use ( {}, []);
array_map(function () ue () {}, []);

// procedures and standalone functions
function test () {}
array_map(function () {}, []);
array_map(function () { 1 + 1;}, []);
array_map(function ($var) { 1 + 1;}, []);
array_map(function ($var) use ($var2) { 1 + 1;}, []);

// multiple statements
array_map(function ($var) { return 1 + 1; 1 + 1; }, []);
array_map(function ($var) { return 1 + 1; return 1 + 1; }, []);
array_map(function ($var) { 1 + 1; return 1 + 1; }, []);

// FOLD

// zero arguments, literal return
array_map(<fold text='{ '>function </fold>()<fold text=' => '> { return </fold>1<fold text=' }'>; }</fold>, []);
array_map(<fold text='{ '>function </fold>()<fold text=' => '> { return </fold>"sdsd"<fold text=' }'>; }</fold>, []);

// zero arguments, expression return
array_map(<fold text='{ '>function </fold>()<fold text=' => '> { return </fold>1 + 1<fold text=' }'>; }</fold>, []);
array_map(<fold text='{ '>function </fold>()<fold text=' => '> { return </fold>$arg + 1<fold text=' }'>; }</fold>, []);

// one argument
array_map(<fold text='{ '>function </fold>($arg1)<fold text=' => '> { return </fold>1 + 1<fold text=' }'>; }</fold>)

// one argument with docblock
array_map(<fold text='{ '>function </fold>(/** @var somettype $arg1  */$arg1)<fold text=' => '> { return </fold>1 + 1<fold text=' }'>; }</fold>)

// multiple arguments
array_map(<fold text='{ '>function </fold>($arg1, $arg2 )<fold text=' => '> { return </fold>1 + 1<fold text=' }'>; }</fold>)

// use, one capture
array_map(<fold text='{ '>function </fold>($arg1, $arg2) use ($var1)<fold text=' => '> { return </fold>1 + 1<fold text=' }'>; }</fold>)

// use, multiple capture
array_map(<fold text='{ '>function </fold>($arg1, $arg2) use ($var1, $var2 )<fold text=' => '> { return </fold>1 + 1<fold text=' }'>; }</fold>)