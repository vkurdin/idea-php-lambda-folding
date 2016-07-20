<?php
namespace PhpOption;

abstract class Option {
    /**
     * @param mixed $arg
     * @return Option
     */
    public static function fromValue($arg) { return new \stdClass; }

    /**
     * @param $callable
     * @return Option
     */
    abstract public function map($callable);

    /**
     * @param $callable
     * @return Option
     */
    abstract public function filter($callable);

    /**
     * @param $callable
     * @return Option
     */
    abstract public function filterNot($callable)

    /**
     * @param $callable
     * @return Option
     */
    abstract public function forAll($callable);
}

class Bar { public $barProperty; }

class Foo { public $fooProperty; }

class FooBar { public $fooBarProperty; }