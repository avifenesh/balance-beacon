import React from 'react'
import { View, Text, StyleSheet, Pressable } from 'react-native'
import type { Account, DbAccountType } from '../../stores/accountsStore'
import { formatCurrency } from '../../utils/format'

const DB_TYPE_TO_LABEL: Record<DbAccountType, string> = {
  SELF: 'Personal',
  PARTNER: 'Partner',
  OTHER: 'Other',
}

interface AccountItemProps {
  account: Account
  isActive: boolean
  isDeleteDisabled: boolean
  onPress: (accountId: string) => void
  onEdit: (account: Account) => void
  onDelete: (account: Account) => void
}

export function AccountItem({
  account,
  isActive,
  isDeleteDisabled,
  onPress,
  onEdit,
  onDelete,
}: AccountItemProps) {
  const currency = account.preferredCurrency || 'USD'

  return (
    <Pressable
      style={[styles.accountCard, isActive && styles.accountCardActive]}
      onPress={() => onPress(account.id)}
      testID={`accounts.account.${account.id}`}
    >
      <View style={styles.accountHeader}>
        <View style={styles.accountInfo}>
          {account.color && (
            <View
              style={[styles.colorIndicator, { backgroundColor: account.color }]}
              testID={`accounts.colorIndicator.${account.id}`}
            />
          )}
          <Text style={styles.accountName}>{account.name}</Text>
          <View style={[styles.typeBadge, account.dbType !== 'SELF' && styles.typeBadgeShared]}>
            <Text style={styles.typeBadgeText}>{DB_TYPE_TO_LABEL[account.dbType]}</Text>
          </View>
        </View>
        {isActive && (
          <View style={styles.activeIndicator} testID={`accounts.active.${account.id}`}>
            <Text style={styles.activeIndicatorText}>Active</Text>
          </View>
        )}
      </View>

      <Text
        style={[
          styles.balance,
          account.balance >= 0 ? styles.balancePositive : styles.balanceNegative,
        ]}
      >
        {formatCurrency(account.balance, currency)}
      </Text>

      <View style={styles.actions}>
        <Pressable
          style={styles.actionButton}
          onPress={() => onEdit(account)}
          testID={`accounts.edit.${account.id}`}
        >
          <Text style={styles.actionButtonText}>Edit</Text>
        </Pressable>
        <Pressable
          style={[styles.actionButton, styles.actionButtonDelete]}
          onPress={() => onDelete(account)}
          disabled={isDeleteDisabled}
          testID={`accounts.delete.${account.id}`}
        >
          <Text
            style={[
              styles.actionButtonText,
              styles.actionButtonDeleteText,
              isDeleteDisabled && styles.actionButtonDisabledText,
            ]}
          >
            Delete
          </Text>
        </Pressable>
      </View>
    </Pressable>
  )
}

const styles = StyleSheet.create({
  accountCard: {
    backgroundColor: '#1e293b',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  accountCardActive: {
    borderColor: '#38bdf8',
    backgroundColor: 'rgba(56,189,248,0.1)',
  },
  accountHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  accountInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flex: 1,
  },
  colorIndicator: {
    width: 16,
    height: 16,
    borderRadius: 8,
  },
  accountName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#fff',
  },
  typeBadge: {
    backgroundColor: 'rgba(56,189,248,0.2)',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  typeBadgeShared: {
    backgroundColor: 'rgba(168,85,247,0.2)',
  },
  typeBadgeText: {
    fontSize: 10,
    fontWeight: '600',
    color: '#38bdf8',
    textTransform: 'uppercase',
  },
  activeIndicator: {
    backgroundColor: '#22c55e',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
  },
  activeIndicatorText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#fff',
  },
  balance: {
    fontSize: 24,
    fontWeight: '700',
    marginBottom: 12,
  },
  balancePositive: {
    color: '#22c55e',
  },
  balanceNegative: {
    color: '#ef4444',
  },
  actions: {
    flexDirection: 'row',
    gap: 8,
  },
  actionButton: {
    backgroundColor: 'rgba(255,255,255,0.05)',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
  },
  actionButtonDelete: {
    backgroundColor: 'rgba(239,68,68,0.1)',
  },
  actionButtonText: {
    fontSize: 14,
    color: '#94a3b8',
    fontWeight: '500',
  },
  actionButtonDeleteText: {
    color: '#ef4444',
  },
  actionButtonDisabledText: {
    opacity: 0.4,
  },
})
