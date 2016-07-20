<?php

include "PhpOptionStub.php";

use PhpOption\Option;

class Bar { public $barProperty; }

class Foo { public $fooProperty; }

class FooBar { public $fooBarProperty; }

$boxedValue = Option::fromValue(new Foo);

$boxedValue
    ->map(function ($a) { return new Bar($a->fooProperty); })
    ->filter(function ($b) { return new FooBar(); })
    ->map(function ($c) { return new FooBar($c->barProperty); })
    ->map(function ($d) { return $d; })
    ->map(function ($e) { return $e-><caret>; });