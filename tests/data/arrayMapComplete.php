<?php

class Foo {
    public $barProperty;
}
/** @var Foo[] $arr */
$arr = [new Foo(), new Foo()];

array_map(function ($val) { return $val-><caret>;}, $arr);
