package com.xiaomi.duckling

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

abstract class UnitSpec extends AnyFunSpec with Matchers with TableDrivenPropertyChecks
