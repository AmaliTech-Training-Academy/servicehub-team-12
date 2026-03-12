class ETLBaseError(Exception):
    """Base class for ETL-related errors."""


class ExtractionError(ETLBaseError):
    """Raised when data extraction fails."""


class TransformationError(ETLBaseError):
    """Raised when a transformation step fails."""


class LoadError(ETLBaseError):
    """Raised when loading data into a target fails."""


class DataValidationError(ETLBaseError):
    """Raised when input data fails validation checks."""
