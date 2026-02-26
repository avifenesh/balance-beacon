import React from 'react'
import {
  View,
  Text,
  StyleSheet,
  Pressable,
  ScrollView,
} from 'react-native'
import type { DbAccountType } from '../../stores'
import type { Currency } from '../../types'
import { FormInput } from '../forms/FormInput'

const ACCOUNT_COLORS = [
  '#22c55e',
  '#3b82f6',
  '#8b5cf6',
  '#f97316',
  '#ec4899',
  '#06b6d4',
  '#ef4444',
  '#84cc16',
  '#6366f1',
  '#14b8a6',
]

const ACCOUNT_TYPES: { value: DbAccountType; label: string }[] = [
  { value: 'SELF', label: 'Personal' },
  { value: 'PARTNER', label: 'Partner' },
  { value: 'OTHER', label: 'Other' },
]

const CURRENCIES: { value: Currency; label: string }[] = [
  { value: 'USD', label: 'USD' },
  { value: 'EUR', label: 'EUR' },
  { value: 'ILS', label: 'ILS' },
]

export interface AccountFormValues {
  name: string
  type: DbAccountType
  color: string | null
  preferredCurrency: Currency | null
}

interface AccountFormProps {
  values: AccountFormValues
  onChange: (key: keyof AccountFormValues, value: any) => void
  errors?: Partial<Record<keyof AccountFormValues, string>>
  isSubmitting?: boolean
  testID?: string
}

export function AccountForm({
  values,
  onChange,
  errors = {},
  isSubmitting = false,
  testID = 'account-form',
}: AccountFormProps) {

  const renderTypeSelector = () => (
    <View style={styles.sectionContainer}>
      <Text style={styles.sectionLabel}>Type</Text>
      <View style={styles.selectorRow}>
        {ACCOUNT_TYPES.map((accountType) => (
          <Pressable
            key={accountType.value}
            style={[
              styles.selectorOption,
              values.type === accountType.value && styles.selectorOptionSelected,
            ]}
            onPress={() => onChange('type', accountType.value)}
            testID={`${testID}.type.${accountType.value}`}
            disabled={isSubmitting}
          >
            <Text
              style={[
                styles.selectorOptionText,
                values.type === accountType.value && styles.selectorOptionTextSelected,
              ]}
            >
              {accountType.label}
            </Text>
          </Pressable>
        ))}
      </View>
    </View>
  )

  const renderColorPicker = () => (
    <View style={styles.sectionContainer}>
      <Text style={styles.sectionLabel}>Color</Text>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.colorPicker}>
        <Pressable
          style={[
            styles.colorOption,
            styles.noColorOption,
            !values.color && styles.colorOptionSelected,
          ]}
          onPress={() => onChange('color', null)}
          testID={`${testID}.color.none`}
          disabled={isSubmitting}
        >
          <Text style={styles.noColorText}>X</Text>
        </Pressable>
        {ACCOUNT_COLORS.map((color) => (
          <Pressable
            key={color}
            style={[
              styles.colorOption,
              { backgroundColor: color },
              values.color === color && styles.colorOptionSelected,
            ]}
            onPress={() => onChange('color', color)}
            testID={`${testID}.color.${color}`}
            disabled={isSubmitting}
          />
        ))}
      </ScrollView>
    </View>
  )

  const renderCurrencySelector = () => (
    <View style={styles.sectionContainer}>
      <Text style={styles.sectionLabel}>Preferred Currency (optional)</Text>
      <View style={styles.selectorRow}>
        <Pressable
          style={[
            styles.selectorOption,
            !values.preferredCurrency && styles.selectorOptionSelected,
          ]}
          onPress={() => onChange('preferredCurrency', null)}
          testID={`${testID}.currency.none`}
          disabled={isSubmitting}
        >
          <Text
            style={[
              styles.selectorOptionText,
              !values.preferredCurrency && styles.selectorOptionTextSelected,
            ]}
          >
            None
          </Text>
        </Pressable>
        {CURRENCIES.map((currency) => (
          <Pressable
            key={currency.value}
            style={[
              styles.selectorOption,
              values.preferredCurrency === currency.value && styles.selectorOptionSelected,
            ]}
            onPress={() => onChange('preferredCurrency', currency.value)}
            testID={`${testID}.currency.${currency.value}`}
            disabled={isSubmitting}
          >
            <Text
              style={[
                styles.selectorOptionText,
                values.preferredCurrency === currency.value &&
                  styles.selectorOptionTextSelected,
              ]}
            >
              {currency.label}
            </Text>
          </Pressable>
        ))}
      </View>
    </View>
  )

  return (
    <View style={styles.container}>
      <FormInput
        label="Name"
        value={values.name}
        onChangeText={(text) => onChange('name', text)}
        error={errors.name}
        placeholder="Enter account name"
        maxLength={50}
        autoFocus
        editable={!isSubmitting}
        testID={`${testID}.nameInput`}
        helperText={`${values.name.length}/50`}
      />

      {renderTypeSelector()}
      {renderColorPicker()}
      {renderCurrencySelector()}
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 16,
  },
  sectionContainer: {
    marginBottom: 16,
  },
  sectionLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#94a3b8',
    marginBottom: 8,
  },
  selectorRow: {
    flexDirection: 'row',
    gap: 8,
  },
  selectorOption: {
    flex: 1,
    paddingVertical: 10,
    alignItems: 'center',
    borderRadius: 8,
    backgroundColor: 'rgba(255,255,255,0.05)',
  },
  selectorOptionSelected: {
    backgroundColor: '#38bdf8',
  },
  selectorOptionText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#94a3b8',
  },
  selectorOptionTextSelected: {
    color: '#0f172a',
  },
  colorPicker: {
    flexDirection: 'row',
  },
  colorOption: {
    width: 36,
    height: 36,
    borderRadius: 18,
    marginRight: 10,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  colorOptionSelected: {
    borderColor: '#fff',
  },
  noColorOption: {
    backgroundColor: 'rgba(255,255,255,0.1)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  noColorText: {
    color: '#94a3b8',
    fontSize: 16,
    fontWeight: '600',
  },
})
