<?php

class Foo {
    public $fooProperty;
}

class Bar {
    public $barProperty;
}

/** @var Foo[] $arr */
$arr = [new Foo(), new Foo()];

array_map(function (Bar $val) { return $val-><caret>;}, $arr);

