@file:OptIn(ExperimentalStdlibApi::class)

package abi49_0_0.expo.modules.kotlin.types

import abi49_0_0.com.facebook.react.bridge.Dynamic
import abi49_0_0.com.facebook.react.bridge.DynamicFromObject
import abi49_0_0.com.facebook.react.bridge.ReadableMap
import abi49_0_0.expo.modules.kotlin.exception.CollectionElementCastException
import abi49_0_0.expo.modules.kotlin.exception.exceptionDecorator
import abi49_0_0.expo.modules.kotlin.jni.ExpectedType
import abi49_0_0.expo.modules.kotlin.recycle
import kotlin.reflect.KType

class MapTypeConverter(
  converterProvider: TypeConverterProvider,
  private val mapType: KType
) : DynamicAwareTypeConverters<Map<*, *>>(mapType.isMarkedNullable) {
  init {
    require(mapType.arguments.first().type?.classifier == String::class) {
      "The map key type should be String, but received ${mapType.arguments.first()}."
    }
  }

  private val valueConverter = converterProvider.obtainTypeConverter(
    requireNotNull(mapType.arguments.getOrNull(1)?.type) {
      "The map type should contain the key type."
    }
  )

  override fun convertFromDynamic(value: Dynamic): Map<*, *> {
    val jsMap = value.asMap()
    return convertFromReadableMap(jsMap)
  }

  override fun convertFromAny(value: Any): Map<*, *> {
    return if (valueConverter.isTrivial()) {
      value as Map<*, *>
    } else {
      (value as Map<*, *>).mapValues { (_, v) ->
        exceptionDecorator({ cause ->
          CollectionElementCastException(
            mapType,
            mapType.arguments[1].type!!,
            v!!::class,
            cause
          )
        }) {
          valueConverter.convert(v)
        }
      }
    }
  }

  private fun convertFromReadableMap(jsMap: ReadableMap): Map<*, *> {
    val result = mutableMapOf<String, Any?>()

    jsMap.entryIterator.forEach { (key, value) ->
      DynamicFromObject(value).recycle {
        exceptionDecorator({ cause ->
          CollectionElementCastException(mapType, mapType.arguments[1].type!!, type, cause)
        }) {
          result[key] = valueConverter.convert(this)
        }
      }
    }

    return result
  }

  override fun getCppRequiredTypes(): ExpectedType = ExpectedType.forMap(
    valueConverter.getCppRequiredTypes()
  )

  override fun isTrivial() = valueConverter.isTrivial()
}
