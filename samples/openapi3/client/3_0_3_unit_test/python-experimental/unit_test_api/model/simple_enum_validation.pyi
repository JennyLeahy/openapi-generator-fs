# coding: utf-8

"""
    openapi 3.0.3 sample spec

    sample spec for testing openapi functionality, built from json schema tests for draft6  # noqa: E501

    The version of the OpenAPI document: 0.0.1
    Generated by: https://openapi-generator.tech
"""

import re  # noqa: F401
import typing  # noqa: F401
import functools  # noqa: F401

import decimal  # noqa: F401
from datetime import date, datetime  # noqa: F401
import uuid  # noqa: F401

import frozendict  # noqa: F401

from unit_test_api import schemas  # noqa: F401


class SimpleEnumValidation(
    schemas.SchemaEnumMakerClsFactory(
        enum_value_to_name={
            1: "POSITIVE_1",
            2: "POSITIVE_2",
            3: "POSITIVE_3",
        }
    ),
    schemas.NumberSchema
):
    """NOTE: This class is auto generated by OpenAPI Generator.
    Ref: https://openapi-generator.tech

    Do not edit the class manually.
    """
    
    @classmethod
    @property
    def POSITIVE_1(cls):
        return cls(1)
    
    @classmethod
    @property
    def POSITIVE_2(cls):
        return cls(2)
    
    @classmethod
    @property
    def POSITIVE_3(cls):
        return cls(3)