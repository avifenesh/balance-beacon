import React, { useEffect, useCallback, useState } from 'react'
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  Pressable,
  ActivityIndicator,
  Alert,
  RefreshControl,
} from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import type { AppStackScreenProps } from '../../navigation/types'
import { useAccountsStore, useToastStore, type Account } from '../../stores'
import { AccountItem } from '../../components/accounts/AccountItem'
import { CreateAccountModal } from '../../components/accounts/CreateAccountModal'
import { EditAccountModal } from '../../components/accounts/EditAccountModal'

export function AccountsScreen({ navigation }: AppStackScreenProps<'Accounts'>) {
  const accounts = useAccountsStore((state) => state.accounts)
  const activeAccountId = useAccountsStore((state) => state.activeAccountId)
  const isLoading = useAccountsStore((state) => state.isLoading)
  const error = useAccountsStore((state) => state.error)

  const [isCreateVisible, setIsCreateVisible] = useState(false)
  const [editingAccount, setEditingAccount] = useState<Account | null>(null)

  useEffect(() => {
    useAccountsStore.getState().fetchAccounts()
  }, [])

  const onRefresh = useCallback(() => {
    useAccountsStore.getState().fetchAccounts()
  }, [])

  const handleClose = useCallback(() => {
    navigation.goBack()
  }, [navigation])

  const handleSwitchAccount = useCallback(async (accountId: string) => {
    if (accountId === activeAccountId) return

    const success = await useAccountsStore.getState().setActiveAccount(accountId)
    if (success) {
      useToastStore.getState().success('Account switched')
    } else {
      const errorMsg = useAccountsStore.getState().error
      useToastStore.getState().error(errorMsg || 'Failed to switch account')
    }
  }, [activeAccountId])

  const handleDeletePress = useCallback((account: Account) => {
    if (account.id === activeAccountId) {
      Alert.alert(
        'Cannot Delete',
        'This is your active account. Switch to another account before deleting.',
        [{ text: 'OK' }]
      )
      return
    }

    if (accounts.length <= 1) {
      Alert.alert(
        'Cannot Delete',
        'You cannot delete your only account.',
        [{ text: 'OK' }]
      )
      return
    }

    Alert.alert(
      'Delete Account',
      `Are you sure you want to delete "${account.name}"? This action cannot be undone.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            const success = await useAccountsStore.getState().deleteAccount(account.id)
            if (success) {
              useToastStore.getState().success('Account deleted')
            } else {
              const errorMsg = useAccountsStore.getState().error
              useToastStore.getState().error(errorMsg || 'Failed to delete account')
            }
          },
        },
      ]
    )
  }, [activeAccountId, accounts.length])

  const renderEmpty = useCallback(() => {
    if (isLoading) return null
    return (
      <View style={styles.emptyContainer} testID="accounts.empty">
        <Text style={styles.emptyText}>No accounts found</Text>
        <Pressable style={styles.emptyButton} onPress={() => setIsCreateVisible(true)} testID="accounts.emptyCreateButton">
          <Text style={styles.emptyButtonText}>Create Account</Text>
        </Pressable>
      </View>
    )
  }, [isLoading])

  return (
    <SafeAreaView style={styles.container} edges={['top']} testID="accounts.screen">
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={handleClose} style={styles.closeButton} testID="accounts.closeButton">
          <Text style={styles.closeText}>Close</Text>
        </Pressable>
        <Text style={styles.title}>Accounts</Text>
        <Pressable onPress={() => setIsCreateVisible(true)} style={styles.addButton} testID="accounts.addButton">
          <Text style={styles.addText}>Add</Text>
        </Pressable>
      </View>

      {/* Error */}
      {error && (
        <View style={styles.errorContainer} testID="accounts.error">
          <Text style={styles.errorText}>{error}</Text>
        </View>
      )}

      {/* Loading */}
      {isLoading && accounts.length === 0 ? (
        <View style={styles.loadingContainer} testID="accounts.loading">
          <ActivityIndicator size="large" color="#38bdf8" />
          <Text style={styles.loadingText}>Loading accounts...</Text>
        </View>
      ) : (
        <FlatList
          data={accounts}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <AccountItem
              account={item}
              isActive={item.id === activeAccountId}
              isDeleteDisabled={item.id === activeAccountId || accounts.length <= 1}
              onPress={handleSwitchAccount}
              onEdit={setEditingAccount}
              onDelete={handleDeletePress}
            />
          )}
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={renderEmpty}
          testID="accounts.list"
          refreshControl={<RefreshControl refreshing={isLoading} onRefresh={onRefresh} tintColor="#38bdf8" />}
        />
      )}

      <CreateAccountModal
        visible={isCreateVisible}
        onClose={() => setIsCreateVisible(false)}
      />

      <EditAccountModal
        visible={!!editingAccount}
        onClose={() => setEditingAccount(null)}
        account={editingAccount}
      />
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.1)',
  },
  closeButton: {
    paddingVertical: 8,
    paddingRight: 16,
  },
  closeText: {
    fontSize: 16,
    color: '#38bdf8',
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
    color: '#fff',
  },
  addButton: {
    paddingVertical: 8,
    paddingLeft: 16,
  },
  addText: {
    fontSize: 16,
    color: '#38bdf8',
    fontWeight: '500',
  },
  errorContainer: {
    backgroundColor: 'rgba(239,68,68,0.1)',
    padding: 12,
    marginHorizontal: 24,
    marginTop: 16,
    borderRadius: 8,
  },
  errorText: {
    color: '#ef4444',
    fontSize: 14,
    textAlign: 'center',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    color: '#94a3b8',
    fontSize: 14,
    marginTop: 12,
  },
  listContent: {
    padding: 24,
    paddingBottom: 100,
  },
  emptyContainer: {
    alignItems: 'center',
    paddingVertical: 48,
  },
  emptyText: {
    color: '#64748b',
    fontSize: 16,
    marginBottom: 16,
  },
  emptyButton: {
    backgroundColor: '#38bdf8',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  emptyButtonText: {
    color: '#0f172a',
    fontSize: 16,
    fontWeight: '600',
  },
})
