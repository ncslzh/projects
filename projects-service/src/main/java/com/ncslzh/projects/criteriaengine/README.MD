# Criteria Engine
The criteria engine is designed to be able to create reusable business defined criteria(s) across multiple domains.

## Summary
A `Criteria` can be modeled after a business required criteria.
Following the English definition where criteria is the plural of criterion, a `Criteria` is a collection of `Criterion`.

A `Criterion` is a single business domain requirement that <u>ideally</u> can not be broken down any further into other `Criterion`.


> To make it less confusing, the terms "Criteria" and "Group of Criterion" can be use interchangeably

We use `CriterionChain` that uses the *chain-of-responsibility* design pattern to evaluate each `Criterion`.

Using predefined configs existing in `CriteriaEngineEnums`, we can further customize
1. The definition of success or failure of an evaluation of a `Criteria` - using `CriteriaExecutionResultStrategy`
2. Early Termination Evaluation for a collection of `Criterion` - `CriterionExecutionTerminationStrategy`

The `CriteriaEvaluationOutput` contains information about the `ExecutionInfo` of each `Criterion` and a combined `ExecutionStats`

Depending on the configured `CriteriaExecutionResultStrategy`, the strategy will return either`CriteriaEngineEnums.CriteriaExecutionResult.PASSED`
or `CriteriaEngineEnums.CriteriaExecutionResult.FAILED`

---
---
## Example Usage

In matching Ads, there are specific business logic that we need to filter out ads.
For this example, we will specifically use the `LowQualityUsersCriteria` as it was the first one to be implemented using the criteria engine.

This criteria is used to filter out Ads whose creators do not meet a certain standard, so that we can improve the quality of Ads being matched, with the
assumption that better quality user = better ads.

There are six criterions that we look at, which is the exact same as `CaMakerStatsValidator`:

1. Minimum account age days            - `MinAccountageDaysCriterion`
2. Minimum completed orders count      - `MinCompletedOrdersCountCriterion`
3. Minimum unique trade partners count - `MinUniqueTradePartnersCountCriterion`
4. Minimum completion rate             - `MinCompletionRateCriterion`
5. Minimum buy orders total value (6 months)  - `MinBuyOrdersTotalValueSixMonthsCriterion`
6. Minimum sell orders total value (6 months) - `MinSellOrdersTotalValueSixMonthsCriterion`
7. Maximum liable order disputes count        - `MaxLiableOrderDisputesCountCriterion`

> These 6 `Criterion` make up a business defined criteria: `LowQualityUsersCriteria`

### Configs

<u>Defining whether a user passes or fails the criteria:</u> `CriteriaExecutionResultStrategy`

According to this business defined criteria, as long as a user **does not** meet any of the criterion, the user will be considered
as "low quality".

> Hence, we should use `CriteriaEngineEnums.CriteriaExecutionResultStrategy.FAIL_IF_ANY_FAIL`

<u>Defining whether evaluation should early terminate or not:</u> `CriterionExecutionTerminationStrategy`

As above, once a user fails any criterion, it is considered as an overall criteria fail.

> Thus, we can use `CriteriaEngineEnums.CriterionExecutionTerminationStrategy.TERMINATE_IF_FAILED`, which is the default strategy.
>
> However, for debug/testing purposes, it may be helpful to use `CriteriaEngineEnums.CriterionExecutionTerminationStrategy.CONTINUE_ALWAYS`
> for evaluating all `Criterion` in the group

---
---
## Adding new business requirements to the Criteria Engine

### Criterion
`Criterion` classes written to be managed by Spring with the `@Component` annotation.
This is so that they can have access to other Spring managed classes to obtain required data when necessary.

#### Defining the class structure
To add a new `Criterion`, you need to ensure it implements `Criterion<T, U>` where
1. `T` is the passed `input` type
2. `U` is the passed `config` type

> In order to ensure that the passed `input` and `config` contains the required fields, add two interfaces
> `RequiredInput` and `RequiredConfig extends Criteria.CriteriaConfig` with relevant getter methods.

Your class should then be something like this:

`class NewCriterion<T extends NewCriterion.RequiredInput, U extends NewCriterion.RequiredConfig> implements Criterion<T, U>`

#### Implementing methods
You must then implement these two methods
1. `shouldCheckCriterion(@NonNull U config)`
2. `matchesCriterionCore(@NonNull T input, @NonNull U config)`

If `shouldCheckCriterion` returns `false`, by default `matchesCriterionCore` will not be called.

See `MinAccountAgeDaysCriterion` for an example

---

### Criteria
`Criteria` classes are also Spring managed with the `@Component` annotation.

#### Defining the class structure
To add a new `Criteria`, you need to ensure it implements `Criteria<T, U extends CriteriaConfig>` where
1. `T` is the defined `input` type.
2. `U` is the defined `config` type.

You will need to create two inner pojo class `CriteriaConfig implements ...` and `CriteriaInput implements ...`
that will fulfil `T` and `U`. More on this below.

#### Adding Criterion(s) to your new Criteria
You will need to ensure that your `Criteria` class has a `CriterionChain<CriteriaInput, CriteriaConfig> criterionChain
that contains the list of `Criterion` to be evaluated. An easy way would be to do something like this

```
class NewCriteria { 
...
  private List<Criterion<CriteriaInput, CriteriaConfig>> criteria;
  private CriterionChain<CriteriaInput, CriteriaConfig> criterionChain;

 @PostConstruct
  public void init() {
    criteria =
        List.of(
            minAccountAgeDaysCriterion,
            minCompletedOrdersCountCriterion); 

    criterionChain =
        new CriterionChain<>(
            CriteriaEngineEnums.CriterionExecutionTerminationStrategy.TERMINATE_IF_FAILED);
    for (Criterion<CriteriaInput, CriteriaConfig> criterion : criteria) {
      criterionChain.addCriterion(criterion);
    }
  }
...
}
```

#### Creating CriteriaConfig and CriteriaInput inner classes
These inner POJO classes are criteria specific, and to be constructed by classes that wants to use the specific `Criteria` class.
This ensures that only required fields are populated by the callers, and only required fields are used for evaluating the criteria.

> The `CriteriaConfig` class will have to implement <u>ALL</u> `RequiredConfig` interfaces of declared `Criterion`s

> The `CriteriaInput` class will have to implement <u>ALL</u> `RequiredInput` interfaces of declared `Criterion`s

Using the above `NewCriteria` example, there are two `Criterions`: `minAccountAgeDaysCriterion` and `minCompletedOrdersCountCriterion`

Thus, the classes should look like this
```
  @Data
  @NoArgsConstructor
  public static class CriteriaConfig
      implements Criteria.CriteriaConfig,
          MinAccountAgeDaysCriterion.RequiredConfig,
          MinCompletedOrdersCountCriterion.RequiredConfig {

    private Integer minAccountAgeDays;
    private Integer minCompletedOrdersCount; 
    private Boolean isDebugLog;
  }
  
  @Data
  @AllArgsConstructor
  @Builder
  public static class CriteriaInput
      implements MinAccountAgeDaysCriterion.RequiredInput,
          MinCompletedOrdersCountCriterion.RequiredInput {

    // Inputs must be provided, but criteria can be null
    // ======== Required Inputs
    @NonNull private Integer accountAgeDays;
    @NonNull private Integer completedOrdersCount; 
    // ======== Required Inputs

    @NonNull private String legalCurrencySymbol;

    // ======== For logging purposes
    @NonNull private Long userId;
    @NonNull private Integer brokerId;
    @NonNull private Long publicAdId;
  }
```
#### Explanation
`CriteriaInput` can have some additional fields (e.g. `userId` in the above case) that is not needed directly for evaluating any `Criterion`

By implementing the interfaces for the required fields of each `Criterion`, we reduce the likelihood of bugs at compile time
by mandating the field to be present in the `input` and `config` objects.

Although it is more verbose, I believe it also helps in readability.

#### Implementing methods
You must then implement these two methods
1. `CriteriaConfig getCriteriaConfig(@NonNull String configName)`
2. `boolean checkCriteria(@NonNull CriteriaInput input, @NonNull CriteriaConfig config)`

> Be careful of calling `getCriteriaConfig` inside `checkCriteria` instead of passing a constructed `CriteriaConfig` as this may result in
> high volumes of `CriteriaConfig` object construction which can reduce API performance

See `LowQualityUsersCriteria` for an example

---
---




